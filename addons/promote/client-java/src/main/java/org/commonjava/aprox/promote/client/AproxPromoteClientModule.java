package org.commonjava.aprox.promote.client;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.client.core.util.UrlUtils;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.promote.model.PromoteRequest;
import org.commonjava.aprox.promote.model.PromoteResult;

public class AproxPromoteClientModule
    extends AproxClientModule
{

    private static final String PROMOTE_BASEPATH = "promotion";

    public PromoteResult promote( final StoreType srcType, final String srcName, final StoreType targetType,
                                  final String targetName, final boolean purgeSource, final String... paths )
        throws AproxClientException
    {
        final PromoteRequest req =
            new PromoteRequest( new StoreKey( srcType, srcName ), new StoreKey( targetType, targetName ),
                                new HashSet<String>( Arrays.asList( paths ) ) ).setPurgeSource( purgeSource );

        final PromoteResult result = http.postWithResponse( promoteUrl(), req, PromoteResult.class );
        return result;
    }

    public PromoteResult promote( final StoreKey src, final StoreKey target, final boolean purgeSource,
                                  final String... paths )
        throws AproxClientException
    {
        final PromoteRequest req =
            new PromoteRequest( src, target, new HashSet<String>( Arrays.asList( paths ) ) ).setPurgeSource( purgeSource );

        final PromoteResult result = http.postWithResponse( promoteUrl(), req, PromoteResult.class );
        return result;
    }

    public PromoteResult promote( final PromoteRequest req )
        throws AproxClientException
    {
        final PromoteResult result = http.postWithResponse( promoteUrl(), req, PromoteResult.class );
        return result;
    }

    public Set<String> getPromotablePaths( final StoreType type, final String name )
        throws AproxClientException
    {
        final StoreKey sk = new StoreKey( type, name );
        final PromoteResult result = promote( new PromoteRequest( sk, sk ).setDryRun( true ) );

        return result.getPendingPaths();
    }

    public PromoteResult resume( final PromoteResult result )
        throws AproxClientException
    {
        return http.postWithResponse( resumeUrl(), result, PromoteResult.class );
    }

    public PromoteResult rollback( final PromoteResult result )
        throws AproxClientException
    {
        return http.postWithResponse( rollbackUrl(), result, PromoteResult.class );
    }

    private String promoteUrl()
    {
        return UrlUtils.buildUrl( http.getBaseUrl(), PROMOTE_BASEPATH, "promote" );
    }

    private String resumeUrl()
    {
        return UrlUtils.buildUrl( http.getBaseUrl(), PROMOTE_BASEPATH, "resume" );
    }

    private String rollbackUrl()
    {
        return UrlUtils.buildUrl( http.getBaseUrl(), PROMOTE_BASEPATH, "rollback" );
    }

}
