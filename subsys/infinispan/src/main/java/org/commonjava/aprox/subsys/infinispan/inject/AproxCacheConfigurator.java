package org.commonjava.aprox.subsys.infinispan.inject;

import java.util.Map;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;

public interface AproxCacheConfigurator
{

    void configure( GlobalConfigurationBuilder globalConfig, ConfigurationBuilder defaultConfig,
                    Map<String, ConfigurationBuilder> namedConfigs );

}
