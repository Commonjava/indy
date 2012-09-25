package org.commonjava.aprox.autoprox.conf;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.conf.AproxFeatureConfig;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigNames;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "autoprox" )
@Named( "use-factory-instead" )
@Singleton
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

    private final String path;

    @ConfigNames( { "path" } )
    public AutoProxConfiguration( final String path )
    {
        this.path = path;
    }

    public final String getPath()
    {
        return path == null ? DEFAULT_PATH : path;
    }

}
