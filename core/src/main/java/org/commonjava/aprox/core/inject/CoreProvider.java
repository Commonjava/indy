package org.commonjava.aprox.core.inject;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.inject.Production;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;

import com.fasterxml.jackson.databind.Module;

@ApplicationScoped
public class CoreProvider
{

    @Inject
    private Instance<Module> objectMapperModules;

    private AproxObjectMapper objectMapper;

    public CoreProvider()
    {
    }

    @PostConstruct
    public void init()
    {
        this.objectMapper = new AproxObjectMapper( objectMapperModules );
    }

    @Produces
    @Default
    @Production
    public AproxObjectMapper getAproxObjectMapper()
    {
        return objectMapper;
    }

}
