package org.commonjava.aprox.core.stats;

import org.commonjava.web.json.ser.JsonSerializer;
import org.junit.Test;

public class AProxVersioningTest
{

    @Test
    public void serializeToJson()
    {
        final String json = new JsonSerializer().toString( new AProxVersioning() );

        System.out.println( json );
    }

}
