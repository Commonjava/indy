package org.commonjava.aprox.tensor.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;

import org.commonjava.aprox.depgraph.util.AproxDepgraphUtils;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.junit.Test;

public class AproxDiscoveryUtilsTest
{

    @Test
    public void parseTypeAndNameFromAproxURI()
        throws Exception
    {
        final URI uri = new URI( "aprox:group:test" );
        final StoreKey key = AproxDepgraphUtils.getDiscoveryStore( uri );

        assertThat( key, notNullValue() );
        assertThat( key.getType(), equalTo( StoreType.group ) );
        assertThat( key.getName(), equalTo( "test" ) );
    }

}
