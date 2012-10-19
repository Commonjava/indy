package org.commonjava.aprox.rest.util;

import java.net.URL;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.commonjava.aprox.conf.AproxConfigFactory;
import org.commonjava.aprox.inject.TestData;
import org.commonjava.aprox.subsys.infinispan.conf.CacheConfiguration;
import org.commonjava.web.config.ConfigurationException;

@javax.enterprise.context.ApplicationScoped
public class TestConfigProvider
{
    @Produces
    @Default
    public AproxConfigFactory getFactory()
    {
        return new AproxConfigFactory()
        {
            @Override
            public <T> T getConfiguration( final Class<T> configCls )
                throws ConfigurationException
            {
                return null;
            }
        };
    }

    @Produces
    @TestData
    @Default
    public CacheConfiguration getCacheConfig()
    {
        final URL resource = Thread.currentThread()
                                   .getContextClassLoader()
                                   .getResource( "infinispan.xml" );
        final CacheConfiguration cc = new CacheConfiguration();
        cc.setPath( resource.getPath() );
        return cc;
    }
}