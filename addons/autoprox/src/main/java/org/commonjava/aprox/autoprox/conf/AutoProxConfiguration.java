package org.commonjava.aprox.autoprox.conf;

public interface AutoProxConfiguration
{

    boolean isEnabled();

    void setEnabled( boolean enabled );

    /**
     * @return NEVER null
     */
    AutoRepoConfiguration getRepo();

    /**
     * @return NEVER null
     */
    AutoDeployConfiguration getDeploy();

    /**
     * @return NEVER null
     */
    AutoGroupConfiguration getGroup();

}
