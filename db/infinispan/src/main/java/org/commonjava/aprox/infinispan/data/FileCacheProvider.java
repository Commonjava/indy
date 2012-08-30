package org.commonjava.aprox.infinispan.data;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.infinispan.conf.CacheConfiguration;
import org.commonjava.shelflife.model.Expiration;
import org.commonjava.shelflife.model.ExpirationKey;
import org.commonjava.shelflife.store.infinispan.ShelflifeCache;
import org.commonjava.util.logging.Logger;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;

@Singleton
public class FileCacheProvider
{

    private static final String STORE_CACHE = "aprox";

    private static final String EXPIRATION_CACHE = "shelflife";

    private final Logger logger = new Logger( getClass() );

    @Inject
    private CacheConfiguration config;

    private CacheContainer container;

    private transient Cache<StoreKey, ArtifactStore> storeCache;

    private transient Cache<ExpirationKey, Expiration> expirationCache;

    @PostConstruct
    public void initCaches()
        throws ProxyDataException
    {
        final File f = new File( config.getPath() );
        if ( !f.exists() )
        {
            throw new ProxyDataException(
                                          "Cannot read infinispan configuration from: %s. Reason: File does not exist.",
                                          config.getPath() );
        }

        logger.info( "\n\n\n\n[APROX-ISPN] Reading Infinispan configuration from: %s", f.getAbsolutePath() );

        FileInputStream fin = null;
        try
        {
            fin = new FileInputStream( f );
            container = new DefaultCacheManager( fin );
            container.start();
        }
        catch ( final IOException e )
        {
            throw new ProxyDataException( "Cannot read infinispan configuration from: %s. Reason: %s.", e,
                                          config.getPath(), e.getMessage() );
        }
        finally
        {
            closeQuietly( fin );
        }

        getStoreCache();
        getShelflifeCache();
    }

    @Produces
    @AproxData
    public CacheContainer getCacheContainer()
    {
        return container;
    }

    @Produces
    @AproxData
    public synchronized Cache<StoreKey, ArtifactStore> getStoreCache()
        throws ProxyDataException
    {
        checkContainer();

        if ( storeCache == null )
        {
            storeCache = container.getCache( STORE_CACHE );
            storeCache.start();
        }

        return storeCache;
    }

    private void checkContainer()
        throws ProxyDataException
    {
        if ( container == null )
        {
            throw new ProxyDataException(
                                          "Cannot retrieve infinispan cache: container has not initialized successfully." );
        }
    }

    @Produces
    @ShelflifeCache
    public Cache<ExpirationKey, Expiration> getShelflifeCache()
        throws ProxyDataException
    {
        checkContainer();

        if ( expirationCache == null )
        {
            expirationCache = container.getCache( EXPIRATION_CACHE );
            expirationCache.start();
        }

        return expirationCache;
    }
}
