package org.commonjava.aprox.autoprox.rest.dto;

import java.util.Collections;
import java.util.List;

import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;

public class AutoProxCalculation
{

    private ArtifactStore store;

    private List<ArtifactStore> supplementalStores;

    private String ruleName;

    public AutoProxCalculation()
    {
    }

    public AutoProxCalculation( final RemoteRepository store, final String ruleName )
    {
        this.store = store;
        this.supplementalStores = null;
        this.ruleName = ruleName;
    }

    public AutoProxCalculation( final HostedRepository store, final String ruleName )
    {
        this.store = store;
        this.supplementalStores = null;
        this.ruleName = ruleName;
    }

    public AutoProxCalculation( final Group store, final List<ArtifactStore> supplementalStores, final String ruleName )
    {
        super();
        this.store = store;
        this.supplementalStores = supplementalStores;
        this.ruleName = ruleName;
    }

    public String getRuleName()
    {
        return ruleName;
    }

    public ArtifactStore getStore()
    {
        return store;
    }

    public List<ArtifactStore> getSupplementalStores()
    {
        return supplementalStores == null ? Collections.<ArtifactStore> emptyList() : supplementalStores;
    }

    public void setStore( final ArtifactStore store )
    {
        this.store = store;
    }

    public void setSupplementalStores( final List<ArtifactStore> supplementalStores )
    {
        this.supplementalStores = supplementalStores;
    }

    public void setRuleName( final String ruleName )
    {
        this.ruleName = ruleName;
    }

}
