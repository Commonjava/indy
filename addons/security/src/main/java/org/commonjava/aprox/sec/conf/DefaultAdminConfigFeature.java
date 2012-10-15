package org.commonjava.aprox.sec.conf;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.conf.AproxFeatureConfig;
import org.commonjava.aprox.inject.Production;
import org.commonjava.badgr.conf.AdminConfiguration;
import org.commonjava.badgr.conf.DefaultAdminConfiguration;
import org.commonjava.web.config.ConfigurationException;

@Singleton
public final class DefaultAdminConfigFeature
    extends AproxFeatureConfig<AdminConfiguration, DefaultAdminConfiguration>
{
    @Inject
    private DefaultAdminConfigInfo info;

    public DefaultAdminConfigFeature()
    {
        super( DefaultAdminConfiguration.class );
    }

    @Produces
    @Production
    @Default
    public AdminConfiguration getCacheConfig()
        throws ConfigurationException
    {
        return getConfig();
    }

    @Override
    public AproxConfigInfo getInfo()
    {
        return info;
    }

    @Singleton
    public static final class DefaultAdminConfigInfo
        extends AproxConfigInfo
    {
        public DefaultAdminConfigInfo()
        {
            super( DefaultAdminConfiguration.class );
        }
    }

}