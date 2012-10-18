package org.commonjava.aprox.tensor.io.ispn;

import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.aprox.subsys.infinispan.inject.AproxCacheConfigurator;
import org.commonjava.tensor.io.ispn.TensorCacheConfigurator;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;

@javax.enterprise.context.ApplicationScoped
public class TensorToAproxConfigBridge
    implements AproxCacheConfigurator
{

    @Inject
    private Instance<TensorCacheConfigurator> tensorConfigs;

    @Override
    public void configure( final GlobalConfigurationBuilder globalConfig, final ConfigurationBuilder defaultConfig,
                           final Map<String, ConfigurationBuilder> namedConfigs )
    {
        for ( final TensorCacheConfigurator config : tensorConfigs )
        {
            config.configure( globalConfig, defaultConfig, namedConfigs );
        }
    }

}
