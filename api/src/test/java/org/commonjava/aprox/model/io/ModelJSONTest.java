package org.commonjava.aprox.model.io;

import static org.junit.Assert.fail;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.web.json.ser.JsonSerializer;
import org.junit.Test;

public class ModelJSONTest
{

    private final JsonSerializer serializer = new JsonSerializer( new StoreKeySerializer() );

    String loadJson( final String resource )
        throws Exception
    {
        final InputStream is = Thread.currentThread()
                                     .getContextClassLoader()
                                     .getResourceAsStream( "model-io/" + resource );
        if ( is == null )
        {
            fail( "Cannot find classpath resource: model-io/" + resource );
        }

        return IOUtils.toString( is );
    }

    @Test
    public void deserializeHostedRepo()
        throws Exception
    {
        final String json = loadJson( "hosted-with-storage.json" );
        System.out.println( json );
        final HostedRepository repo = serializer.fromString( json, HostedRepository.class );
        System.out.println( repo );
    }

}
