/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.bind.jaxrs;

import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.o11yphant.metrics.annotation.Measure;
import org.commonjava.o11yphant.metrics.MetricsConstants;
import org.commonjava.o11yphant.metrics.DefaultMetricsManager;
import org.commonjava.indy.util.RequestContextHelper;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
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
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.commonjava.indy.util.RequestContextHelper.CLIENT_ADDR;
import static org.commonjava.indy.util.RequestContextHelper.CUMULATIVE_COUNTS;
import static org.commonjava.indy.util.RequestContextHelper.CUMULATIVE_TIMINGS;
import static org.commonjava.indy.util.RequestContextHelper.IS_METERED;
import static org.commonjava.indy.util.RequestContextHelper.REQUEST_PHASE;
import static org.commonjava.indy.util.RequestContextHelper.REQUEST_PHASE_START;
import static org.commonjava.indy.util.RequestContextHelper.X_FORWARDED_FOR;

@ApplicationScoped
public class ResourceManagementFilter
        implements Filter
{

    public static final String HTTP_REQUEST = "http-request";

    public static final String ORIGINAL_THREAD_NAME = "original-thread-name";

    public static final String METHOD_PATH_TIME = "method-path-time";

    private static final String BASE_CONTENT_METRIC = "indy.content.";

    private static final String POM_CONTENT_METRIC = BASE_CONTENT_METRIC + "pom";

    private static final String NORMAL_CONTENT_METRIC = BASE_CONTENT_METRIC + "other";

    private static final String METADATA_CONTENT_METRIC = BASE_CONTENT_METRIC + "metadata";

    private static final String SPECIAL_CONTENT_METRIC = BASE_CONTENT_METRIC + "special";

    private static final String FORCE_METERED = "force-metered";

    @Inject
    private CacheProvider cacheProvider;

    @Inject
    private MDCManager mdcManager;

    @Inject
    private SpecialPathManager specialPathManager;

    @Inject
    private DefaultMetricsManager metricsManager;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Logger restLogger = LoggerFactory.getLogger( "org.commonjava.topic.rest.inbound" );

    @Override
    public void init( final FilterConfig filterConfig )
            throws ServletException
    {
        if ( logger.isTraceEnabled() )
        {
            cacheProvider.startReporting();
        }
    }

    @Override
    @Measure
    public void doFilter( final ServletRequest request, final ServletResponse response, final FilterChain chain )
            throws IOException, ServletException
    {
        logger.trace( "START: {}", getClass().getSimpleName() );

        final HttpServletRequest hsr = (HttpServletRequest) request;

        String name = Thread.currentThread().getName();

        String tn = hsr.getMethod() + " " + hsr.getPathInfo() + " (" + System.currentTimeMillis() + "." + System.nanoTime() + ")";
        String qs = hsr.getQueryString();

        String clientAddr = hsr.getRemoteAddr();
        final String xForwardFor = hsr.getHeader( X_FORWARDED_FOR );
        if ( xForwardFor != null )
        {
            clientAddr = xForwardFor; // OSE proxy use HTTP header 'x-forwarded-for' to represent user IP
        }

        try
        {
            ThreadContext threadContext = ThreadContext.getContext( true );

            boolean isMetered = metricsManager.isMetered( ()-> RequestContextHelper.getContext( FORCE_METERED, Boolean.FALSE ) );

            threadContext.put( IS_METERED, isMetered );

            mdcManager.putUserIP( clientAddr );
            mdcManager.putExtraHeaders( hsr );
            mdcManager.putRequestIDs( hsr );

            threadContext.put( ORIGINAL_THREAD_NAME, name );

            threadContext.put( HTTP_REQUEST, hsr );

            threadContext.put( METHOD_PATH_TIME, tn );

            logger.debug( "START request: {} (from: {})", tn, clientAddr );

            Thread.currentThread().setName( tn );

            RequestContextHelper.setContext( REQUEST_PHASE, REQUEST_PHASE_START );
            restLogger.info( "START {}{} (from: {})", hsr.getRequestURL(), qs == null ? "" : "?" + qs, clientAddr );
            MDC.remove( REQUEST_PHASE );

            AtomicReference<IOException> ioex = new AtomicReference<>();
            AtomicReference<ServletException> seex = new AtomicReference<>();

            metricsManager.wrapWithStandardMetrics( () -> {
                try
                {
                    chain.doFilter( request, response );
                }
                catch ( IOException e )
                {
                    ioex.set( e );
                }
                catch ( ServletException e )
                {
                    seex.set( e );
                }
                return null;
            }, pathClassifier( hsr.getPathInfo() ) );

            if ( ioex.get() != null )
            {
                throw ioex.get();
            }

            if ( seex.get() != null )
            {
                throw seex.get();
            }
        }
        finally
        {
            logger.debug( "Cleaning up resources for thread: {}", Thread.currentThread().getName() );
            try
            {
                cacheProvider.cleanupCurrentThread();
            }
            catch ( Exception e )
            {
                logger.error( "Failed to cleanup resources", e );
            }

            ThreadContext ctx = ThreadContext.getContext( false );
            if ( ctx != null )
            {
                Map<String, Double> cumulativeTimings = (Map<String, Double>) ctx.get( CUMULATIVE_TIMINGS );
                if ( cumulativeTimings != null )
                {
                    cumulativeTimings.forEach(
                            ( k, v ) -> RequestContextHelper.setContext( CUMULATIVE_TIMINGS + "." + k, String.format( "%.3f", v ) ) );
                }

                Map<String, Integer> cumulativeCounts = (Map<String, Integer>) ctx.get( CUMULATIVE_COUNTS );
                if ( cumulativeCounts != null )
                {
                    cumulativeCounts.forEach(
                            ( k, v ) -> RequestContextHelper.setContext( CUMULATIVE_COUNTS + "." + k, String.format( "%d", v ) ) );
                }
            }

            restLogger.info( "END {}{} (from: {})", hsr.getRequestURL(), qs == null ? "" : "?" + qs, clientAddr );

            Thread.currentThread().setName( name );

            logger.debug( "END request: {} (from: {})", tn, clientAddr );

            mdcManager.clear();

            logger.trace( "END: {}", getClass().getSimpleName() );
        }
    }

    private Supplier<String> pathClassifier( final String pathInfo )
    {
        return ()->{
            if ( !pathInfo.contains( "content" ))
            {
                return MetricsConstants.SKIP_METRIC;
            }
            SpecialPathInfo spi = specialPathManager.getSpecialPathInfo( pathInfo );
            if ( spi == null )
            {
                return pathInfo.endsWith( ".pom" ) ? POM_CONTENT_METRIC : NORMAL_CONTENT_METRIC;
            }
            else if ( spi.isMetadata() )
            {
                return METADATA_CONTENT_METRIC;
            }
            else
            {
                return SPECIAL_CONTENT_METRIC;
            }
        };
    }

    @Override
    public void destroy()
    {
    }

}
