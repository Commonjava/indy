package org.commonjava.aprox.core.inject;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.io.StoreKeySerializer;
import org.commonjava.web.json.ser.JsonSerializer;

@ApplicationScoped
public class AproxJsonSerializerProducer
{

    private JsonSerializer serializer;

    @Produces
    @AproxData
    @Default
    public synchronized JsonSerializer getSerializer()
    {
        if ( serializer == null )
        {
            serializer = new JsonSerializer( new StoreKeySerializer() );
        }

        return serializer;
    }

}
