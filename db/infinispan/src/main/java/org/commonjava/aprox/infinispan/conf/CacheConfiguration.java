package org.commonjava.aprox.infinispan.conf;

import javax.inject.Named;
import javax.inject.Singleton;

import org.commonjava.aprox.core.conf.AproxConfigSet;
import org.commonjava.web.config.annotation.ConfigNames;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "infinispan" )
@Named( "use-factory-instead" )
public class CacheConfiguration
{
    @Singleton
    public static final class ConfigSet
        extends AproxConfigSet<CacheConfiguration, CacheConfiguration>
    {
        public ConfigSet()
        {
            super( CacheConfiguration.class );
        }
    }

    public static final String DEFAULT_PATH = "/etc/aprox/infinispan.xml";

    private final String path;

    @ConfigNames( { "path" } )
    public CacheConfiguration( final String path )
    {
        this.path = path;
    }

    public final String getPath()
    {
        return path;
    }

}
