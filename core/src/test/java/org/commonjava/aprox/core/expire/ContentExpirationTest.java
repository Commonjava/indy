package org.commonjava.aprox.core.expire;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.junit.Test;

public class ContentExpirationTest
{

    @Test
    public void roundTrip()
        throws Exception
    {
        final ContentExpiration exp =
            new ContentExpiration( new StoreKey( StoreType.remote, "test" ), "/path/to/something.good" );
        final AproxObjectMapper mapper = new AproxObjectMapper( false );

        final String json = mapper.writeValueAsString( exp );
        System.out.println( json );

        final ContentExpiration result = mapper.readValue( json, ContentExpiration.class );

        assertThat( result, equalTo( exp ) );
    }

}
