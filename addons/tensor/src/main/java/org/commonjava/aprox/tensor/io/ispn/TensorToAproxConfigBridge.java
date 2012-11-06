package org.commonjava.aprox.tensor.io.ispn;

import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.aprox.subsys.infinispan.inject.AproxCacheConfigurator;
import org.commonjava.tensor.io.ispn.TensorCacheConfigurator;
import org.commonjava.util.logging.Logger;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;

@javax.enterprise.context.ApplicationScoped
public class TensorToAproxConfigBridge
    implements AproxCacheConfigurator // see if leaving this out improves performance.
{

    @Inject
    private Instance<TensorCacheConfigurator> tensorConfigs;

    @Override
    public void configure( final GlobalConfigurationBuilder globalConfig, final ConfigurationBuilder defaultConfig,
                           final Map<String, ConfigurationBuilder> namedConfigs )
    {
        new Logger( getClass() ).info( "Running tenso-bridge cache configurator: %s", getClass().getName() );
        for ( final TensorCacheConfigurator config : tensorConfigs )
        {
            new Logger( getClass() ).info( "Running tensor cache configurator: %s", config.getClass()
                                                                                          .getName() );
            config.configure( globalConfig, defaultConfig, namedConfigs );
        }
    }

}
