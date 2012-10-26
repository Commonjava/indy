package org.commonjava.aprox.infinispan.model.io;

import java.util.Map;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.SerializationConfigurationBuilder;

public class DBCacheConfigurator
//    implements AproxCacheConfigurator // see if this improves performance.
{

    //    @Override
    public void configure( final GlobalConfigurationBuilder globalConfig, final ConfigurationBuilder defaultConfig,
                           final Map<String, ConfigurationBuilder> namedConfigs )
    {
        final SerializationConfigurationBuilder ser = globalConfig.serialization();

        ser.addAdvancedExternalizer( 1000, new JsonStoreKeyExternalizer() );
        ser.addAdvancedExternalizer( 1001, new JsonRepositoryExternalizer() );
        ser.addAdvancedExternalizer( 1002, new JsonGroupExternalizer() );
        ser.addAdvancedExternalizer( 1003, new JsonDeployPointExternalizer() );
    }

}
