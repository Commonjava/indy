package org.commonjava.indy.repo.proxy.content;

import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class RepoProxyCacheProducer
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CacheProducer cacheProducer;

    private static final String REPO_PROXY_CONTENT_CACHE = "repo-proxy-content";

    @RepoProxyContentCache
    @Produces
    @ApplicationScoped
    public CacheHandle<String, String> scheduleExpireCache()
    {
        return cacheProducer.getCache( REPO_PROXY_CONTENT_CACHE );
    }
}
