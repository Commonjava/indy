package org.commonjava.aprox.autoprox.conf;

import java.util.List;

import org.commonjava.aprox.core.model.StoreKey;

public interface AutoProxConfiguration
{

    String getProxyBase();

    List<StoreKey> getExtraGroupConstituents();

    boolean isDeploymentCreationEnabled();

    boolean isEnabled();

    void setEnabled( boolean enabled );

}
