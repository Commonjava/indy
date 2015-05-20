package org.commonjava.aprox.implrepo.inject;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.implrepo.data.ImpliedRepoMetadataManager;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;

@ApplicationScoped
public class ImpliedRepoProvider
{
    private ImpliedRepoMetadataManager metadataManager;

    @Inject
    private AproxObjectMapper mapper;

    @PostConstruct
    public void setup()
    {
        metadataManager = new ImpliedRepoMetadataManager( mapper );
    }

    @Produces
    @Default
    public ImpliedRepoMetadataManager getImpliedRepoMetadataManager()
    {
        return metadataManager;
    }
}
