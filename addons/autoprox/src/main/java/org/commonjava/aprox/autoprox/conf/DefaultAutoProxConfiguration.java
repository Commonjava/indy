package org.commonjava.aprox.autoprox.conf;

import org.commonjava.util.logging.Logger;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;
import org.commonjava.web.config.section.ConfigurationSectionListener;

@SectionName( ConfigurationSectionListener.DEFAULT_SECTION )
public class DefaultAutoProxConfiguration
    implements AutoProxConfiguration
{

    public final Logger logger = new Logger( getClass() );

    private AutoDeployConfiguration deploy;

    private AutoRepoConfiguration repo;

    private boolean enabled = true;

    private AutoGroupConfiguration group;

    public DefaultAutoProxConfiguration( final AutoProxConfiguration original, final AutoRepoConfiguration repo,
                                         final AutoDeployConfiguration deploy, final AutoGroupConfiguration group )
    {
        this.group = group;
        this.deploy = deploy;
        this.repo = repo;
        this.enabled = original.isEnabled();
    }

    public DefaultAutoProxConfiguration( final boolean enabled, final AutoRepoConfiguration repo,
                                         final AutoDeployConfiguration deploy, final AutoGroupConfiguration group )
    {
        this.group = group;
        this.deploy = deploy;
        this.repo = repo;
        this.enabled = enabled;
    }

    public DefaultAutoProxConfiguration( final String baseUrl )
    {
        this.repo = new DefaultAutoRepoConfiguration( baseUrl );
    }

    public DefaultAutoProxConfiguration()
    {
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    @ConfigName( "enabled" )
    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    @Override
    public AutoRepoConfiguration getRepo()
    {
        if ( repo == null )
        {
            throw new NullPointerException(
                                            "Missing [repository] section of configuration! You must supply AT LEAST this section, with a 'base.url' parameter!" );
        }

        return repo;
    }

    @Override
    public AutoDeployConfiguration getDeploy()
    {
        return deploy == null ? new DefaultAutoDeployConfiguration() : deploy;
    }

    @Override
    public AutoGroupConfiguration getGroup()
    {
        return group == null ? new DefaultAutoGroupConfiguration() : group;
    }

}
