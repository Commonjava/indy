package org.commonjava.aprox.autoprox.rest.dto;

import java.util.List;

import org.commonjava.aprox.autoprox.data.RuleMapping;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;

public class AutoProxCalculation
{

    private final ArtifactStore store;

    private final List<ArtifactStore> supplementalStores;

    private final String ruleName;

    public AutoProxCalculation( final RemoteRepository store, final RuleMapping mapping )
    {
        this.store = store;
        this.supplementalStores = null;
        this.ruleName = mapping.getScriptName();
    }

    public AutoProxCalculation( final HostedRepository store, final RuleMapping mapping )
    {
        this.store = store;
        this.supplementalStores = null;
        this.ruleName = mapping.getScriptName();
    }

    public AutoProxCalculation( final Group store, final List<ArtifactStore> supplementalStores,
                                final RuleMapping mapping )
    {
        super();
        this.store = store;
        this.supplementalStores = supplementalStores;
        this.ruleName = mapping.getScriptName();
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
        return supplementalStores;
    }

}
