package org.commonjava.indy.core.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

@Named( "ISPN-data-restore" )
public class ISPNDataRestoreAction
                implements MigrationAction
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyConfiguration config;

    @Inject
    private CacheProducer cacheProducer;

    @Override
    public String getId()
    {
        return "Restore ISPN caches";
    }

    @Override
    public int getMigrationPriority()
    {
        return 95;
    }

    @Override
    public boolean migrate() throws IndyLifecycleException
    {
        File dir = new File( config.getIndyHomeDir(), "var/lib/indy/data/infinispan" );
        if ( !dir.isDirectory() )
        {
            logger.info( "Infinispan restore dir not exist, dir: {}", dir );
            return true;
        }

        String[] files = dir.list();
        for ( String file : files )
        {
            File f = new File( dir, file );
            String cacheName = file.replace( ".gz", "" ); // remove .gz if there
            try
            {
                restore( cacheName, f );
            }
            catch ( Exception e )
            {
                throw new IndyLifecycleException( "Failed to restore cache, name: " + cacheName, e );
            }
        }
        return true;
    }

    private void restore( String cacheName, File f ) throws Exception
    {
        CacheHandle<Object, Object> cache = cacheProducer.getCache( cacheName );

        AtomicReference<Exception> error = new AtomicReference<>();
        try (ObjectInputStream in = new ObjectInputStream( new GZIPInputStream( new FileInputStream( f ) ) ))
        {
            cache.executeCache( ( c ) -> {
                try
                {
                    long records = in.readLong();

                    for ( long i = 0; i < records; i++ )
                    {
                        try
                        {
                            Object k = in.readObject();
                            Object v = in.readObject();

                            c.putAsync( k, v );
                        }
                        catch ( Exception e )
                        {
                            logger.error( "Failed to read entry at index: " + i, e );
                            error.set( e );
                        }
                    }
                    logger.info( "Load {} complete, size: {}", cacheName, records );
                }
                catch ( IOException e )
                {
                    logger.error( "Failed to read data file header.", e );
                    error.set( e );
                }
                return true;
            } );
        }

        if ( error.get() != null )
        {
            throw error.get();
        }
    }
}
