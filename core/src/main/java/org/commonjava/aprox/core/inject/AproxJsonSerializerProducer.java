package org.commonjava.aprox.core.inject;

import javax.enterprise.inject.Produces;

import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.io.StoreKeySerializer;
import org.commonjava.web.json.ser.JsonSerializer;

@javax.enterprise.context.ApplicationScoped
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
