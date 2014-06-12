package org.commonjava.aprox.autoprox.rest.dto;

import java.util.List;

import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;

public class AutoProxCalculation
{

    private final ArtifactStore store;

    private final List<ArtifactStore> supplementalStores;

    public AutoProxCalculation( final RemoteRepository store )
    {
        this.store = store;
        this.supplementalStores = null;
    }

    public AutoProxCalculation( final HostedRepository store )
    {
        this.store = store;
        this.supplementalStores = null;
    }

    public AutoProxCalculation( final Group store, final List<ArtifactStore> supplementalStores )
    {
        super();
        this.store = store;
        this.supplementalStores = supplementalStores;
    }

    public ArtifactStore getStore()
    {
        return store;
    }

    public List<ArtifactStore> getSupplementalStores()
    {
        return supplementalStores;
    }

}
