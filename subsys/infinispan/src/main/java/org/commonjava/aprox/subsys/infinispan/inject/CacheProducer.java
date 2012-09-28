package org.commonjava.aprox.subsys.infinispan.inject;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.subsys.infinispan.conf.CacheConfiguration;
import org.commonjava.util.logging.Logger;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

@Singleton
public class CacheProducer
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private CacheConfiguration config;

    private EmbeddedCacheManager container;

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
    @Default
    public EmbeddedCacheManager getCacheContainer()
    {
        return container;
    }

}
