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
package org.commonjava.aprox.autoprox.client;

import org.commonjava.aprox.autoprox.rest.dto.AutoProxCalculation;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.client.core.util.UrlUtils;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;

public class AutoProxCalculatorModule
    extends AproxClientModule
{

    public AutoProxCalculation calculateRuleOutput( final StoreType type, final String name )
        throws AproxClientException
    {
        return http.get( UrlUtils.buildUrl( "autoprox/eval", type.singularEndpointName(), name ),
                         AutoProxCalculation.class );
    }

    public AutoProxCalculation calculateRuleOutput( final StoreKey key )
        throws AproxClientException
    {
        return http.get( UrlUtils.buildUrl( "autoprox/eval", key.getType()
                                                                .singularEndpointName(), key.getName() ),
                         AutoProxCalculation.class );
    }

}
