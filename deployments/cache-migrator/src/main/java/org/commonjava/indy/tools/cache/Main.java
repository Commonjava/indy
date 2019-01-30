package org.commonjava.indy.tools.cache;

import org.apache.commons.io.FileUtils;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.boot.IndyBootException;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.commonjava.indy.boot.BootInterface.ERR_CANT_INIT_BOOTER;
import static org.commonjava.indy.boot.BootInterface.ERR_CANT_PARSE_ARGS;

public class Main
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Weld weld;

    private WeldContainer container;

    private CacheProducer producer;

    public static void main( String[] args )
    {
        Thread.currentThread()
              .setUncaughtExceptionHandler( ( thread, error ) -> {
                  if ( error instanceof InvocationTargetException )
                  {
                      final InvocationTargetException ite = (InvocationTargetException) error;
                      System.err.println( "In: " + thread.getName() + "(" + thread.getId()
                                                  + "), caught InvocationTargetException:" );
                      ite.getTargetException()
                         .printStackTrace();

                      System.err.println( "...via:" );
                      error.printStackTrace();
                  }
                  else
                  {
                      System.err.println( "In: " + thread.getName() + "(" + thread.getId() + ") Uncaught error:" );
                      error.printStackTrace();
                  }
              } );

        MigrationOptions options = new MigrationOptions();
        try
        {
            if ( options.parseArgs( args ) )
            {
                try
                {
                    int result = new Main().run( options );
                    if ( result != 0 )
                    {
                        System.exit( result );
                    }
                }
                catch ( final IndyBootException e )
                {
                    System.err.printf( "ERROR INITIALIZING BOOTER: %s", e.getMessage() );
                    System.exit( ERR_CANT_INIT_BOOTER );
                }
            }
        }
        catch ( final IndyBootException e )
        {
            System.err.printf( "ERROR: %s", e.getMessage() );
            System.exit( ERR_CANT_PARSE_ARGS );
        }
    }

    private int run( final MigrationOptions options )
            throws IndyBootException
    {
        try
        {
            File inXml = options.getInfinispanXml();
            File outXmlDir = new File( System.getProperty("java.io.tmpdir", "/tmp"), "infinispan-config-" + System.currentTimeMillis());
            if ( !outXmlDir.isDirectory() && !outXmlDir.mkdirs() )
            {
                throw new IndyBootException(
                        "Failed to create temporary direcory for infinispan configuration loading" );
            }

            File outXml = new File( outXmlDir, "infinispan.xml" );
            FileUtils.copyFile( inXml, outXml );

            Properties props = System.getProperties();

            props.setProperty( "indy.config.dir", outXmlDir.getAbsolutePath() );

            System.setProperties( props );

            weld = new Weld();
            weld.property("org.jboss.weld.se.archive.isolation", false);

            // Weld shutdown hook might be called before Indy's, we need to disable it to allow Indy's shutdown hooks execute smoothly
            weld.skipShutdownHook();

            container = weld.initialize();

            producer = container.select( CacheProducer.class ).get();

            CacheHandle<Object, Object> cache = producer.getCache( options.getCacheName() );
            if ( MigrationCommand.dump == options.getMigrationCommand() )
            {
                AtomicReference<Throwable> error = new AtomicReference<>();
                try (ObjectOutputStream out = new ObjectOutputStream( new GZIPOutputStream( new FileOutputStream( options.getDataFile() ) )))
                {
                    cache.executeCache( ( c ) -> {
                        try
                        {
                            out.writeLong( c.size() );
                        }
                        catch ( IOException e )
                        {
                            logger.error( "Failed to write data file header.", e );
                            error.set( e );
                        }

                        if ( error.get() == null )
                        {
                            c.forEach( ( k, v ) -> {
                                if ( error.get() == null )
                                {
                                    try
                                    {
                                        out.writeObject( k );
                                        out.writeObject( v );
                                    }
                                    catch ( IOException e )
                                    {
                                        logger.error( "Failed to write entry with key: " + k, e );
                                        error.set( e );
                                    }
                                }
                            });
                        }

                        return true;
                    } );
                }
                catch ( IOException e )
                {
                    error.set( e );
                }

                if ( error.get() != null )
                {
                    throw new IndyBootException( "Failed to write data to file: " + options.getDataFile(), error.get() );
                }
            }
            else
            {
                AtomicReference<Throwable> error = new AtomicReference<>();
                try (ObjectInputStream in = new ObjectInputStream(
                        new GZIPInputStream( new FileInputStream( options.getDataFile() ) ) ))
                {
                    cache.executeCache( (c)->{
                        try
                        {
                            long records = in.readLong();

                            for(long i=0; i<records; i++)
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
                        }
                        catch ( IOException e )
                        {
                            logger.error( "Failed to read data file header.", e );
                            error.set( e );
                        }

                        return true;
                    } );
                }
                catch ( IOException e )
                {
                    error.set( e );
                }

                if ( error.get() != null )
                {
                    throw new IndyBootException( "Failed to read data from file: " + options.getDataFile(), error.get() );
                }
            }

        }
        catch ( final Throwable e )
        {
            if ( e instanceof IndyBootException )
                throw (IndyBootException)e;

            logger.error( "Failed to initialize Booter: " + e.getMessage(), e );
            return ERR_CANT_INIT_BOOTER;
        }
        finally
        {
            if ( container != null )
            {
                try
                {
                    producer.stop();
                }
                catch ( final IndyLifecycleException e )
                {
                    logger.error( "Failed to stop cache subsystem: " + e.getMessage(), e );
                }

                weld.shutdown();
            }
        }

        return 0;
    }
}
