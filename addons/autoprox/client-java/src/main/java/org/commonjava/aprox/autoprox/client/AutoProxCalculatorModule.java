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
