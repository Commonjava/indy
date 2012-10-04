package org.commonjava.aprox.infinispan.model.io;

import java.util.Map;

import org.commonjava.aprox.subsys.infinispan.inject.AproxCacheConfigurator;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.SerializationConfigurationBuilder;

public class DBCacheConfigurator
    implements AproxCacheConfigurator
{

    @Override
    public void configure( final GlobalConfigurationBuilder globalConfig, final ConfigurationBuilder defaultConfig,
                           final Map<String, ConfigurationBuilder> namedConfigs )
    {
        final SerializationConfigurationBuilder ser = globalConfig.serialization();

        ser.addAdvancedExternalizer( new JsonDeployPointExternalizer() );
        ser.addAdvancedExternalizer( new JsonGroupExternalizer() );
        ser.addAdvancedExternalizer( new JsonRepositoryExternalizer() );
        ser.addAdvancedExternalizer( new JsonStoreKeyExternalizer() );
    }

}
