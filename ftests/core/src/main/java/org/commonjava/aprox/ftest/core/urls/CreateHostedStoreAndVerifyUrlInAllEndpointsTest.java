package org.commonjava.aprox.ftest.core.urls;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.dto.EndpointView;
import org.commonjava.aprox.model.core.dto.EndpointViewListing;
import org.junit.Test;

public class CreateHostedStoreAndVerifyUrlInAllEndpointsTest
    extends AbstractCoreUrlsTest
{

    @Test
    public void verifyHostedStoreUrlsEndpoints()
        throws Exception
    {
        final EndpointViewListing endpoints = client.stats()
                                                    .getAllEndpoints();
        for ( final EndpointView endpoint : endpoints )
        {
            final String endpointUrl = client.content()
                                             .contentUrl( endpoint.getStoreKey() );

            assertThat( "Resource URI: '" + endpoint.getResourceUri() + "' for endpoint: " + endpoint.getKey()
                + " should be: '" + endpointUrl + "'", endpoint.getResourceUri(), equalTo( endpointUrl ) );
        }
    }

}
