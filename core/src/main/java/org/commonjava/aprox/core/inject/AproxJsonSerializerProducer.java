package org.commonjava.aprox.core.inject;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.io.StoreKeySerializer;
import org.commonjava.web.json.ser.JsonSerializer;

@Singleton
public class AproxJsonSerializerProducer
{

    private JsonSerializer serializer;

    @Produces
    @AproxData
    public synchronized JsonSerializer getSerializer()
    {
        if ( serializer == null )
        {
            serializer = new JsonSerializer( new StoreKeySerializer() );
        }

        return serializer;
    }

}
