package org.commonjava.aprox.subsys.infinispan.inject;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.subsys.infinispan.conf.CacheConfiguration;
import org.commonjava.shelflife.model.Expiration;
import org.commonjava.shelflife.model.ExpirationKey;
import org.commonjava.shelflife.store.infinispan.ShelflifeCache;
import org.commonjava.util.logging.Logger;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;

@Singleton
public class CacheProducer
{

    public static final String EXPIRATION_CACHE = "shelflife";

    private final Logger logger = new Logger( getClass() );

    @Inject
    private CacheConfiguration config;

    private CacheContainer container;

    @PostConstruct
    public void load()
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
    }

    @Produces
    public CacheContainer getCacheContainer()
    {
        return container;
    }

    @Produces
    @AproxData
    public <K, V> Cache<K, V> getCache( final InjectionPoint point )
    {
        final Named named = point.getAnnotated()
                                 .getAnnotation( Named.class );
        if ( named == null )
        {
            throw new IllegalArgumentException( "@AproxData cache injection must have @Named annotation." );
        }

        final Cache<K, V> cache = container.getCache( named.value() );
        cache.start();

        return cache;
    }

    @Produces
    @ShelflifeCache
    public Cache<ExpirationKey, Expiration> getShelflifeCache()
        throws ProxyDataException
    {
        final Cache<ExpirationKey, Expiration> cache = container.getCache( EXPIRATION_CACHE );
        cache.start();

        return cache;
    }
}
