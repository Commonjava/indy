/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.tools.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.FileUtils;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.propulsor.boot.BootException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.commonjava.propulsor.boot.BootStatus.ERR_INIT;
import static org.commonjava.propulsor.boot.BootStatus.ERR_PARSE_ARGS;

public class Main
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private SimpleCacheProducer producer;

    private ObjectMapper objectMapper;

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
                catch ( final BootException e )
                {
                    System.err.printf( "ERROR INITIALIZING BOOTER: %s", e.getMessage() );
                    System.exit( ERR_INIT );
                }
            }
        }
        catch ( final BootException e )
        {
            System.err.printf( "ERROR: %s", e.getMessage() );
            System.exit( ERR_PARSE_ARGS );
        }
    }

    private int run( final MigrationOptions options )
            throws BootException
    {
        try
        {
            File inXml = options.getInfinispanXml();
            if ( inXml != null )
            {
                File outXmlDir = new File( System.getProperty("java.io.tmpdir", "/tmp"), "infinispan-config-" + System.currentTimeMillis());
                if ( !outXmlDir.isDirectory() && !outXmlDir.mkdirs() )
                {
                    throw new BootException(
                            "Failed to create temporary direcory for infinispan configuration loading" );
                }

                File outXml = new File( outXmlDir, "infinispan.xml" );
                FileUtils.copyFile( inXml, outXml );

                Properties props = System.getProperties();

                props.setProperty( "indy.config.dir", outXmlDir.getAbsolutePath() );

                System.setProperties( props );
            }

            producer = new SimpleCacheProducer();
            objectMapper = new IndyObjectMapper(true);
            objectMapper.disable( SerializationFeature.INDENT_OUTPUT );

            CacheHandle<Object, Object> cache = producer.getCache( options.getCacheName() );
            if ( MigrationCommand.dump == options.getMigrationCommand() )
            {

                if ( DataType.json == options.getDataType() )
                {
                    dumpJsonFile( cache,  options );
                }
                else
                {
                    dumpObjectFile( cache, options );
                }

            }
            else
            {
                if ( DataType.json == options.getDataType() )
                {
                    loadFromJsonFile( cache, options );
                }
                else
                {
                    loadFromObjectFile( cache, options );
                }

            }

        }
        catch ( final Throwable e )
        {
            if ( e instanceof BootException )
                throw (BootException)e;

            logger.error( "Failed to initialize Booter: " + e.getMessage(), e );
            return ERR_INIT;
        }
        finally
        {
            try
            {
                producer.stop();
            }
            catch ( final IndyLifecycleException e )
            {
                logger.error( "Failed to stop cache subsystem: " + e.getMessage(), e );
            }
        }

        return 0;
    }

    private void loadFromJsonFile( CacheHandle<Object, Object> cache, MigrationOptions options ) throws BootException
    {
        AtomicReference<Throwable> error = new AtomicReference<>();
        try (BufferedReader in = new BufferedReader( new InputStreamReader(
                        new FileInputStream( options.getDataFile() )  )))
        {
            cache.executeCache( (c)->{
                try
                {
                    String key;
                    int count = 0;
                    while ( (key = in.readLine()) != null )
                    {
                        try
                        {
                            Object k = objectMapper.readValue( key, TrackingKey.class );
                            Object v = objectMapper.readValue( in.readLine(), TrackedContent.class );

                            c.putAsync( k, v );
                            count++;
                        }
                        catch ( Exception e )
                        {
                            logger.error( "Failed to read entry key: {}", key, e );
                            error.set( e );
                        }
                    }
                    logger.info( "Load entries: {}", count );
                }
                catch ( Exception e )
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
            throw new BootException( "Failed to read data from file: " + options.getDataFile(), error.get() );
        }
    }

    private void loadFromObjectFile( CacheHandle<Object, Object> cache, MigrationOptions options ) throws BootException
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
                    logger.info( "Load {} complete, size: {}", options.getCacheName(), records );
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
            throw new BootException( "Failed to read data from file: " + options.getDataFile(), error.get() );
        }
    }

    private void dumpObjectFile( CacheHandle<Object, Object> cache, MigrationOptions options ) throws BootException
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
            throw new BootException( "Failed to write data to file: " + options.getDataFile(), error.get() );
        }
    }

    private void dumpJsonFile( CacheHandle<Object, Object> cache,  MigrationOptions options ) throws BootException
    {
        AtomicReference<Throwable> error = new AtomicReference<>();
        try (BufferedOutputStream out =  new BufferedOutputStream( new FileOutputStream( options.getDataFile() )   ))
        {
            String lineSeparator = System.getProperty("line.separator");

            cache.executeCache( ( c ) -> {

                if ( error.get() == null )
                {
                    c.forEach( ( k, v ) -> {
                        if ( error.get() == null )
                        {
                            try
                            {
                                out.write( objectMapper.writeValueAsBytes( k ) );
                                out.write( lineSeparator.getBytes() );
                                out.write( objectMapper.writeValueAsBytes( v ) );
                                out.write( lineSeparator.getBytes() );
                                out.flush();
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
            throw new BootException( "Failed to write data to file: " + options.getDataFile(), error.get() );
        }
    }
}
