package org.commonjava.indy.metrics;

import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.indy.IndyRequestConstants;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.galley.CacheOnlyLocation;
import org.commonjava.indy.model.galley.GroupLocation;
import org.commonjava.indy.model.galley.RepositoryLocation;
import org.commonjava.indy.pkg.PackageTypeConstants;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.join;
import static org.commonjava.indy.metrics.TrafficClassifierConstants.FN_CONTENT;
import static org.commonjava.indy.metrics.TrafficClassifierConstants.FN_CONTENT_LISTING;
import static org.commonjava.indy.metrics.TrafficClassifierConstants.FN_CONTENT_MAVEN;
import static org.commonjava.indy.metrics.TrafficClassifierConstants.FN_CONTENT_NPM;
import static org.commonjava.indy.metrics.TrafficClassifierConstants.FN_MAVEN_DOWNLOAD;
import static org.commonjava.indy.metrics.TrafficClassifierConstants.FN_MAVEN_UPLOAD;
import static org.commonjava.indy.metrics.TrafficClassifierConstants.FN_METADATA;
import static org.commonjava.indy.metrics.TrafficClassifierConstants.FN_METADATA_MAVEN;
import static org.commonjava.indy.metrics.TrafficClassifierConstants.FN_METADATA_NPM;
import static org.commonjava.indy.metrics.TrafficClassifierConstants.FN_NPM_DOWNLOAD;
import static org.commonjava.indy.metrics.TrafficClassifierConstants.FN_NPM_UPLOAD;
import static org.commonjava.indy.metrics.TrafficClassifierConstants.FN_PROMOTION;
import static org.commonjava.indy.metrics.TrafficClassifierConstants.FN_REPO_MGMT;
import static org.commonjava.indy.metrics.TrafficClassifierConstants.FN_TRACKING_RECORD;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_NPM;

@ApplicationScoped
public class TrafficClassifier
{
    private static final String CACHED_FUNCTIONS = "cached-functions";

    private static final Set<String> MODIFY_METHODS = new HashSet<>( asList( "POST", "PUT", "DELETE" ) );

    private static final Set<String> FOLO_RECORD_ENDPOINTS = new HashSet<>( asList( "record", "report" ) );

    private static final Set<String> DEPRECATED_CONTENT_ENDPOINTS =
            new HashSet<>( asList( "group", "hosted", "remote" ) );


    private SpecialPathManager specialPathManager;

    @Inject
    public TrafficClassifier( SpecialPathManager specialPathManager )
    {
        this.specialPathManager = specialPathManager;
    }

    public List<String> classifyFunctions( String restPath, String method )
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx != null )
        {
            List<String> cached = (List<String>) ctx.get( CACHED_FUNCTIONS );
            if ( cached != null )
            {
                return cached;
            }
        }

        List<String> result = new ArrayList<>();

        String[] pathParts = restPath.split("/" );
        if ( pathParts.length >= 2 )
        {
            String[] classifierParts = new String[pathParts.length - 1];
            System.arraycopy( pathParts, 1, classifierParts, 0, classifierParts.length );

            String restPrefix = join( classifierParts, '/' );

            if ( "promotion".equals( classifierParts[0] ) && "promote".equals( classifierParts[2] ) )
            {
                // this is a promotion request
                result = singletonList( FN_PROMOTION );
            }
            else if ( "admin".equals( classifierParts[0] ) && "stores".equals( classifierParts[1] )
                    && classifierParts.length > 2 )
            {
                if ( MODIFY_METHODS.contains( method ) )
                {
                    // this is a store modification request
                    result = singletonList( FN_REPO_MGMT );
                }
            }
            else if ( "browse".equals( classifierParts[0] ) )
            {
                // this is a browse / list request
                result = singletonList( FN_CONTENT_LISTING );
            }
            else if ( ( "content".equals( classifierParts[0] ) && classifierParts.length >= 4 && ( restPath.endsWith( "/" )
                    || restPath.endsWith( IndyRequestConstants.LISTING_HTML_FILE ) ) ) )
            {
                // this is an old version of the browse / list request
                result = singletonList( FN_CONTENT_LISTING );
            }
            else if ( ( DEPRECATED_CONTENT_ENDPOINTS.contains( classifierParts[0] ) && ( restPath.endsWith( "/" )
                    || restPath.endsWith( IndyRequestConstants.LISTING_HTML_FILE ) ) ) )
            {
                // this is an old, OLD version of the browse / list request
                result = singletonList( FN_CONTENT_LISTING );
            }
            else if ( restPrefix.startsWith( "folo/admin/" ) && FOLO_RECORD_ENDPOINTS.contains( classifierParts[3] ) )
            {
                // this is a request for a tracking record
                result = singletonList( FN_TRACKING_RECORD );
            }
            else if ( restPrefix.startsWith( "folo/track/" ) && classifierParts.length > 6 )
            {
                final String packageType = classifierParts[3];
                final String storeType = classifierParts[4];
                if ( isValidContent( packageType, storeType ) )
                {
                    boolean isMetadata = isMetadata( packageType, storeType, classifierParts[5], pathParts, 7 );
                    final List<String> fns = handleContentFns( isMetadata, packageType, method );
                    if ( !fns.isEmpty() )
                    {
                        result = fns;
                    }
                }
            }
            else if ( "content".equals( classifierParts[0] ) && classifierParts.length > 4 )
            {
                final String packageType = classifierParts[1];
                final String storeType = classifierParts[2];
                if ( isValidContent( packageType, storeType ) )
                {
                    boolean isMetadata = isMetadata( packageType, storeType, classifierParts[3], pathParts, 5 );
                    final List<String> fns = handleContentFns( isMetadata, packageType, method );
                    if ( !fns.isEmpty() )
                    {
                        result = fns;
                    }
                }
            }
            else if ( DEPRECATED_CONTENT_ENDPOINTS.contains( classifierParts[0] ) && classifierParts.length > 2 )
            {
                final String packageType = PKG_TYPE_MAVEN;
                final String storeType = classifierParts[0];
                if ( isValidContent( packageType, storeType ) )
                {
                    boolean isMetadata = isMetadata( packageType, storeType, classifierParts[1], pathParts, 2 );
                    final List<String> fns = handleContentFns( isMetadata, packageType, method );
                    if ( !fns.isEmpty() )
                    {
                        result = fns;
                    }
                }
            }
        }

        ctx.put( CACHED_FUNCTIONS, result );
        return result;
    }

    private List<String> handleContentFns( final boolean isMetadata, final String packageType, final String method )
    {
        final ArrayList<String> fns = new ArrayList<>();
        if ( PKG_TYPE_MAVEN.equals( packageType ) )
        {
            if ( isMetadata )
            {
                fns.addAll( asList( FN_METADATA, FN_METADATA_MAVEN ) );
            }
            else
            {
                fns.addAll( asList( FN_CONTENT, FN_CONTENT_MAVEN ) );
            }
            if ( "PUT".equals( method ) || "POST".equals( method ) )
            {
                fns.add( FN_MAVEN_UPLOAD );
            }
            if ( "GET".equals( method ) )
            {
                fns.add( FN_MAVEN_DOWNLOAD );
            }
        }
        else if ( PKG_TYPE_NPM.equals( packageType ) )
        {
            if ( isMetadata )
            {
                fns.addAll( asList( FN_METADATA, FN_METADATA_NPM ) );
            }
            else
            {
                fns.addAll( asList( FN_CONTENT, FN_CONTENT_NPM ) );
            }
            if ( "PUT".equals( method ) || "POST".equals( method ) )
            {
                fns.add( FN_NPM_UPLOAD );
            }
            if ( "GET".equals( method ) )
            {
                fns.add( FN_NPM_DOWNLOAD );
            }
        }
        return fns;
    }

    private boolean isValidContent( final String packageType, final String storeType )
    {
        return PackageTypeConstants.isValidPackageType( packageType ) && StoreType.get( storeType ) != null;
    }

    private boolean isMetadata( final String packageType, final String storeType, final String storeName, final String[] pathParts, final int realPathStartIdx )
    {
        Location location = getLightweightLocation( packageType, storeType, storeName );

        String[] realPathParts = new String[pathParts.length - realPathStartIdx];
        System.arraycopy( pathParts, realPathStartIdx, realPathParts, 0, realPathParts.length );

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

    public List<String> getFunctionClassifiers()
    {
        ThreadContext ctx = ThreadContext.getContext( false );
        if ( ctx != null )
        {
            return (List<String>) ctx.get( CACHED_FUNCTIONS );
        }

        return null;
    }
}
