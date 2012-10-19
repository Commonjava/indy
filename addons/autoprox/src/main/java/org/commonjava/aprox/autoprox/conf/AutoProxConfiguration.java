package org.commonjava.aprox.autoprox.conf;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxConfigInfo;
import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "autoprox" )
@Named( "use-factory-instead" )
@Alternative
public class AutoProxConfiguration
{
    @ApplicationScoped
    public static class AutoProxFeatureConfig
        extends AbstractAproxFeatureConfig<AutoProxConfiguration, AutoProxConfiguration>
    {
        @Inject
        private AutoProxConfigInfo info;

        public AutoProxFeatureConfig()
        {
            super( AutoProxConfiguration.class );
        }

        @Produces
        @Default
        public AutoProxConfiguration getCacheConfig()
            throws ConfigurationException
        {
            return getConfig();
        }

        @Override
        public AproxConfigInfo getInfo()
        {
            return info;
        }
    }

    @ApplicationScoped
    public static class AutoProxConfigInfo
        extends AbstractAproxConfigInfo
    {
        public AutoProxConfigInfo()
        {
            super( AutoProxConfiguration.class );
        }
    }

    public static final String DEFAULT_PATH = "/etc/aprox/autoprox.json";

    private String path;

    private boolean enabled;

    private boolean deployEnabled;

    @ConfigName( "path" )
    public void setPath( final String path )
    {
        this.path = path;
    }

    public String getPath()
    {
        return path == null ? DEFAULT_PATH : path;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    @ConfigName( "enabled" )
    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    public boolean isDeployEnabled()
    {
        return deployEnabled;
    }

    @ConfigName( "deployEnabled" )
    public void setDeployEnabled( final boolean deployEnabled )
    {
        this.deployEnabled = deployEnabled;
    }

}
