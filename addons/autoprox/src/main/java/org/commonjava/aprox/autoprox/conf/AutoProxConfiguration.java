package org.commonjava.aprox.autoprox.conf;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.conf.AproxFeatureConfig;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "autoprox" )
@Named( "use-factory-instead" )
@Alternative
public class AutoProxConfiguration
{
    @Singleton
    public static final class AutoProxFeatureConfig
        extends AproxFeatureConfig<AutoProxConfiguration, AutoProxConfiguration>
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

    @Singleton
    public static final class AutoProxConfigInfo
        extends AproxConfigInfo
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
    public final void setPath( final String path )
    {
        this.path = path;
    }

    public final String getPath()
    {
        return path == null ? DEFAULT_PATH : path;
    }

    public final boolean isEnabled()
    {
        return enabled;
    }

    @ConfigName( "enabled" )
    public final void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    public final boolean isDeployEnabled()
    {
        return deployEnabled;
    }

    @ConfigName( "deployEnabled" )
    public final void setDeployEnabled( final boolean deployEnabled )
    {
        this.deployEnabled = deployEnabled;
    }

}
