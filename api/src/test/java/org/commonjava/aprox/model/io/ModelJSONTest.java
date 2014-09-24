package org.commonjava.aprox.model.io;

import static org.junit.Assert.fail;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.model.HostedRepository;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ModelJSONTest
{

    private final ObjectMapper mapper = new AproxObjectMapper( true );

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
        final HostedRepository repo = mapper.readValue( json, HostedRepository.class );
        System.out.println( repo );
    }

}
