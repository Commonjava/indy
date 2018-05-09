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
package org.commonjava.indy.autoprox.client;

import org.commonjava.indy.autoprox.rest.dto.AutoProxCalculation;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;

public class AutoProxCalculatorModule
    extends IndyClientModule
{

    public AutoProxCalculation calculateRuleOutput( final String packageType, final StoreType type, final String name )
            throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "autoprox/eval", packageType, type.singularEndpointName(), name ),
                         AutoProxCalculation.class );
    }

    @Deprecated
    public AutoProxCalculation calculateRuleOutput( final StoreType type, final String name )
        throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "autoprox/eval", type.singularEndpointName(), name ),
                         AutoProxCalculation.class );
    }

    public AutoProxCalculation calculateRuleOutput( final StoreKey key )
        throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "autoprox/eval", key.getPackageType(), key.getType()
                                                                .singularEndpointName(), key.getName() ),
                         AutoProxCalculation.class );
    }

}
