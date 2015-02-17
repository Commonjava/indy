package org.commonjava.aprox.bind.jaxrs;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.util.ApplicationContent;

import com.fasterxml.jackson.databind.ObjectMapper;

@Provider
@Produces( ApplicationContent.application_json )
@ApplicationScoped
public class AproxResteasyJsonProvider
    implements ContextResolver<ObjectMapper>
{
    @Inject
    private AproxObjectMapper mapper;

    @Override
    public ObjectMapper getContext( final Class<?> type )
    {
        return mapper;
    }

}
