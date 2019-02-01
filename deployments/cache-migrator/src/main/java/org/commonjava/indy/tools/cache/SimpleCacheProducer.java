package org.commonjava.indy.tools.cache;

import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.commonjava.indy.subsys.infinispan.config.ISPNRemoteConfiguration;

public class SimpleCacheProducer extends CacheProducer
{
    public SimpleCacheProducer()
    {
        super( new DefaultIndyConfiguration(), null, new ISPNRemoteConfiguration() );
        start();
    }
}
