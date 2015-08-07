package org.commonjava.aprox.bind.jaxrs.jackson;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.commonjava.aprox.bind.jaxrs.RestProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

@Consumes( { "application/json", "application/*+json", "text/json" } )
@Produces( { "application/json", "application/*+json", "text/json" } )
public class CDIJacksonProvider
    extends JacksonJsonProvider
    implements RestProvider
{

    @Inject
    private ObjectMapper mapper;

    @Override
    public ObjectMapper locateMapper( final Class<?> type, final MediaType mediaType )
    {
        if ( mapper == null )
        {
            final CDI<Object> cdi = CDI.current();
            return cdi.select( ObjectMapper.class )
                      .get();
        }

        return mapper;
    }
}
