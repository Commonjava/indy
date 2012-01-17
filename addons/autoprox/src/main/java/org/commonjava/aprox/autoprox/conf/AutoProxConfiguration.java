package org.commonjava.aprox.autoprox.conf;

import java.util.List;

import org.commonjava.aprox.core.model.StoreKey;

public interface AutoProxConfiguration
{

    String getBaseUrl();

    List<StoreKey> getExtraGroupConstituents();

    boolean isDeploymentAllowed();

    boolean isEnabled();

    void setEnabled( boolean enabled );

}
