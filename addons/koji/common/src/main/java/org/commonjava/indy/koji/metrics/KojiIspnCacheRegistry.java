package org.commonjava.indy.koji.metrics;

import org.commonjava.indy.koji.content.CachedKojiContentProvider;
import org.commonjava.indy.subsys.infinispan.metrics.IspnCacheRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Set;

@ApplicationScoped
public class KojiIspnCacheRegistry implements IspnCacheRegistry
{
    private Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CachedKojiContentProvider kojiContentProvider;

    @Override
    public Set<String> getCacheNames()
    {
        Set<String> ret = kojiContentProvider.getCacheNames();
        logger.info( "[Metrics] Register Koji query cache, names: {}", ret );
        return ret;
    }
}
