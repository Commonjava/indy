package org.commonjava.aprox.subsys.infinispan.conf;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxConfigInfo;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "infinispan" )
@Named( "use-factory-instead" )
@Alternative
public class CacheConfiguration
{
    @javax.enterprise.context.ApplicationScoped
    public static class CacheFeatureConfig
        extends AbstractAproxFeatureConfig<CacheConfiguration, CacheConfiguration>
    {
        @Inject
        private CacheConfigInfo info;

        public CacheFeatureConfig()
        {
            super( CacheConfiguration.class );
        }

        @Produces
        @Default
        public CacheConfiguration getCacheConfig()
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

    @javax.enterprise.context.ApplicationScoped
    public static class CacheConfigInfo
        extends AbstractAproxConfigInfo
    {
        public CacheConfigInfo()
        {
            super( CacheConfiguration.class );
        }
    }

    public static final String DEFAULT_PATH = "/etc/aprox/infinispan.xml";

    private String path;

    @ConfigName( "path" )
    public void setPath( final String path )
    {
        this.path = path;
    }

    public String getPath()
    {
        return path == null ? DEFAULT_PATH : path;
    }

}
