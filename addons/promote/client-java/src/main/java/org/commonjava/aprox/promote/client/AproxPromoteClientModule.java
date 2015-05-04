/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.promote.client;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpStatus;
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

    public static final String PROMOTE_BASEPATH = "promotion";

    public static final String PROMOTE_PATH = PROMOTE_BASEPATH + "/promote";

    public static final String RESUME_PATH = PROMOTE_BASEPATH + "/resume";

    public static final String ROLLBACK_PATH = PROMOTE_BASEPATH + "/rollback";

    public PromoteResult promote( final StoreType srcType, final String srcName, final StoreType targetType,
                                  final String targetName, final boolean purgeSource, final String... paths )
        throws AproxClientException
    {
        final PromoteRequest req =
            new PromoteRequest( new StoreKey( srcType, srcName ), new StoreKey( targetType, targetName ),
                                new HashSet<String>( Arrays.asList( paths ) ) ).setPurgeSource( purgeSource );

        final PromoteResult result = http.postWithResponse( PROMOTE_PATH, req, PromoteResult.class, HttpStatus.SC_OK );
        return result;
    }

    public PromoteResult promote( final StoreKey src, final StoreKey target, final boolean purgeSource,
                                  final String... paths )
        throws AproxClientException
    {
        final PromoteRequest req =
            new PromoteRequest( src, target, new HashSet<String>( Arrays.asList( paths ) ) ).setPurgeSource( purgeSource );

        final PromoteResult result = http.postWithResponse( PROMOTE_PATH, req, PromoteResult.class, HttpStatus.SC_OK );
        return result;
    }

    public PromoteResult promote( final PromoteRequest req )
        throws AproxClientException
    {
        final PromoteResult result = http.postWithResponse( PROMOTE_PATH, req, PromoteResult.class, HttpStatus.SC_OK );
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
        return http.postWithResponse( RESUME_PATH, result, PromoteResult.class, HttpStatus.SC_OK );
    }

    public PromoteResult rollback( final PromoteResult result )
        throws AproxClientException
    {
        return http.postWithResponse( ROLLBACK_PATH, result, PromoteResult.class, HttpStatus.SC_OK );
    }

    public String promoteUrl()
    {
        return UrlUtils.buildUrl( http.getBaseUrl(), PROMOTE_PATH );
    }

    public String resumeUrl()
    {
        return UrlUtils.buildUrl( http.getBaseUrl(), RESUME_PATH );
    }

    public String rollbackUrl()
    {
        return UrlUtils.buildUrl( http.getBaseUrl(), ROLLBACK_PATH );
    }

}
