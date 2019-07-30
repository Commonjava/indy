/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.sli.jaxrs;

import org.commonjava.indy.IndyRequestConstants;
import org.commonjava.indy.bind.jaxrs.RequestContextHelper;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.galley.CacheOnlyLocation;
import org.commonjava.indy.model.galley.GroupLocation;
import org.commonjava.indy.model.galley.RepositoryLocation;
import org.commonjava.indy.sli.metrics.GoldenSignalsFunctionMetrics;
import org.commonjava.indy.sli.metrics.GoldenSignalsMetricSet;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.join;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.REQUEST_LATENCY_NS;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_NPM;
import static org.commonjava.indy.sli.metrics.GoldenSignalsMetricSet.FN_CONTENT;
import static org.commonjava.indy.sli.metrics.GoldenSignalsMetricSet.FN_CONTENT_LISTING;
import static org.commonjava.indy.sli.metrics.GoldenSignalsMetricSet.FN_CONTENT_MAVEN;
import static org.commonjava.indy.sli.metrics.GoldenSignalsMetricSet.FN_CONTENT_NPM;
import static org.commonjava.indy.sli.metrics.GoldenSignalsMetricSet.FN_METADATA;
import static org.commonjava.indy.sli.metrics.GoldenSignalsMetricSet.FN_METADATA_MAVEN;
import static org.commonjava.indy.sli.metrics.GoldenSignalsMetricSet.FN_METADATA_NPM;
import static org.commonjava.indy.sli.metrics.GoldenSignalsMetricSet.FN_PROMOTION;
import static org.commonjava.indy.sli.metrics.GoldenSignalsMetricSet.FN_REPO_MGMT;
import static org.commonjava.indy.sli.metrics.GoldenSignalsMetricSet.FN_TRACKING_RECORD;

@ApplicationScoped
public class GoldenSignalsFilter
    implements Filter
{
    private static final Set<String> MODIFY_METHODS = new HashSet<>( asList( "POST", "PUT", "DELETE" ) );

    private static final Set<String> FOLO_RECORD_ENDPOINTS = new HashSet<>( asList( "record", "report" ) );

    private static final Set<String> DEPRECATED_CONTENT_ENDPOINTS =
            new HashSet<>( asList( "group", "hosted", "remote" ) );

    @Inject
    private GoldenSignalsMetricSet metricSet;

    @Inject
    private SpecialPathManager specialPathManager;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void init( final FilterConfig filterConfig )
    {
    }

    @Override
    public void doFilter( final ServletRequest servletRequest, final ServletResponse servletResponse,
                          final FilterChain filterChain )
            throws IOException, ServletException
    {
        long start = System.nanoTime();

        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse resp = (HttpServletResponse) servletResponse;
        try
        {
            Set<String> functions = new HashSet<>( getFunctions( req.getPathInfo(), req.getMethod() ) );
            functions.forEach( function -> metricSet.function( function ).ifPresent(
                    GoldenSignalsFunctionMetrics::started ) );
        }
        catch ( Exception e )
        {
            logger.error( "Failed to classify / measure load for: " + req.getPathInfo(), e );
        }

        try
        {
            filterChain.doFilter( req, resp );
        }
        catch ( IOException | ServletException | RuntimeException e )
        {
            new HashSet<>( getFunctions( req.getPathInfo(), req.getMethod() ) ).forEach(
                    function -> metricSet.function( function ).ifPresent( GoldenSignalsFunctionMetrics::error ) );
            throw e;
        }
        finally
        {
            long end = RequestContextHelper.getRequestEndNanos();
            MDC.put( REQUEST_LATENCY_NS, String.valueOf( end - start ) );

            Set<String> functions = new HashSet<>( getFunctions( req.getPathInfo(), req.getMethod() ) );
            boolean error = resp.getStatus() > 499;

            functions.forEach( function -> metricSet.function( function ).ifPresent( ms -> {
                ms.latency( end-start ).call();
                if ( error )
                {
                    ms.error();
                }
            } ) );
        }
    }

    private List<String> getFunctions( String restPath, String method )
    {
        String[] pathParts = restPath.split("/" );
        if ( pathParts.length < 2 )
        {
            return emptyList();
        }

        String[] classifierParts = new String[pathParts.length-1];
        System.arraycopy( pathParts, 1, classifierParts, 0, classifierParts.length );

        String restPrefix = join( classifierParts, '/' );

        if ( "promotion".equals( classifierParts[0] ) && "promote".equals( classifierParts[2] ) )
        {
            // this is a promotion request
            return singletonList( FN_PROMOTION );
        }
        else if ( "admin".equals( classifierParts[0] ) && "stores".equals( classifierParts[1] )
                && classifierParts.length > 2 )
        {
            if ( MODIFY_METHODS.contains( method ) )
            {
                // this is a store modification request
                return singletonList( FN_REPO_MGMT );
            }
        }
        else if ( "browse".equals( classifierParts[0] ) )
        {
            // this is a browse / list request
            return singletonList( FN_CONTENT_LISTING );
        }
        else if ( ( "content".equals( classifierParts[0] ) && classifierParts.length > 4 && ( restPath.endsWith( "/" )
                || restPath.endsWith( IndyRequestConstants.LISTING_HTML_FILE ) ) ) )
        {
            // this is an old version of the browse / list request
            return singletonList( FN_CONTENT_LISTING );
        }
        else if ( ( DEPRECATED_CONTENT_ENDPOINTS.contains( classifierParts[0] ) && ( restPath.endsWith( "/" )
                || restPath.endsWith( IndyRequestConstants.LISTING_HTML_FILE ) ) ) )
        {
            // this is an old, OLD version of the browse / list request
            return singletonList( FN_CONTENT_LISTING );
        }
        else if ( restPrefix.startsWith( "folo/admin/" ) && FOLO_RECORD_ENDPOINTS.contains( classifierParts[3] ) )
        {
            // this is a request for a tracking record
            return singletonList( FN_TRACKING_RECORD );
        }
        else if ( restPrefix.startsWith( "folo/track/" ) && classifierParts.length > 6 )
        {
            String packageType = classifierParts[3];
            boolean isMetadata = isMetadata( packageType, classifierParts[4], classifierParts[5], pathParts, 6 );

            if ( PKG_TYPE_MAVEN.equals( packageType ) )
            {
                return isMetadata ? asList( FN_METADATA, FN_METADATA_MAVEN ) : asList( FN_CONTENT, FN_CONTENT_MAVEN );
            }
            else if ( PKG_TYPE_NPM.equals( packageType ) )
            {
                return isMetadata ? asList( FN_METADATA, FN_METADATA_NPM ) : asList( FN_CONTENT, FN_CONTENT_NPM );
            }
        }
        else if ( "content".equals( classifierParts[0] ) && classifierParts.length > 4 )
        {
            String packageType = classifierParts[1];
            boolean isMetadata = isMetadata( packageType, classifierParts[2], classifierParts[3], pathParts, 5 );

            if ( PKG_TYPE_MAVEN.equals( packageType ) )
            {
                return isMetadata ? asList( FN_METADATA, FN_METADATA_MAVEN ) : asList( FN_CONTENT, FN_CONTENT_MAVEN );
            }
            else if ( PKG_TYPE_NPM.equals( packageType ) )
            {
                return isMetadata ? asList( FN_METADATA, FN_METADATA_NPM ) : asList( FN_CONTENT, FN_CONTENT_NPM );
            }
        }
        else if ( DEPRECATED_CONTENT_ENDPOINTS.contains( classifierParts[0] ) && classifierParts.length > 2 )
        {
            boolean isMetadata = isMetadata( PKG_TYPE_MAVEN, classifierParts[0], classifierParts[1], pathParts, 2 );
            return isMetadata ? asList( FN_METADATA, FN_METADATA_MAVEN ) : asList( FN_CONTENT, FN_CONTENT_MAVEN );
        }

        return emptyList();
    }

    private boolean isMetadata( final String packageType, final String storeType, final String storeName, final String[] pathParts, final int realPathStartIdx )
    {
        Location location = getLightweightLocation( packageType, storeType, storeName );

        String[] realPathParts = new String[pathParts.length-(realPathStartIdx+1)];
        System.arraycopy( pathParts, 2, realPathParts, 0, realPathParts.length );

        String realPath = join( realPathParts, '/' );

        SpecialPathInfo specialPathInfo = specialPathManager.getSpecialPathInfo( location, realPath, packageType );

        return specialPathInfo != null && specialPathInfo.isMetadata();
    }

    private Location getLightweightLocation( final String packageType, final String storeType, final String storeName )
    {
        StoreType st = StoreType.get( storeType );
        switch ( st )
        {
            case remote:
                return new RepositoryLocation( new RemoteRepository( packageType, storeName, "http://used.to.classify.requests.only/" ) );
            case hosted:
                return new CacheOnlyLocation( new HostedRepository( packageType, storeName ) );
            default:
                return new GroupLocation( packageType, storeName );
        }
    }

    @Override
    public void destroy()
    {
    }

}
