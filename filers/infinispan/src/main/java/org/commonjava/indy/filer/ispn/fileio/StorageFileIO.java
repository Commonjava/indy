/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.filer.ispn.fileio;

import org.apache.commons.io.FileUtils;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.filter.KeyFilter;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.persistence.spi.AdvancedCacheLoader;
import org.infinispan.persistence.spi.AdvancedCacheWriter;
import org.infinispan.persistence.spi.InitializationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * AdvancedCacheLoader / AdvancedCacheWriter implementation designed to preserve the directory structure (if not the exact content!)
 * of the storage filesystem. This enables things like new File(..).exists() to work properly.
 *
 * Created by jdcasey on 3/11/16.
 */
@ApplicationScoped
public class StorageFileIO
        implements AdvancedCacheLoader<String, byte[]>, AdvancedCacheWriter<String, byte[]>
{
    public static final String STORAGE_ROOT_DIR = "storage-root";

    private String storageRoot;

    @Override
    public void init( InitializationContext ctx )
    {
        StoreConfiguration configuration = ctx.getConfiguration();
        Properties properties = configuration.properties();

        storageRoot = properties.getProperty( STORAGE_ROOT_DIR );
        if ( storageRoot == null )
        {
            throw new RuntimeException( "No " + STORAGE_ROOT_DIR + " property provided for cache! Cannot initialize "
                                                + getClass().getName() );
        }
    }

    @Override
    public void write( MarshalledEntry<? extends String, ? extends byte[]> entry )
    {
        String key = entry.getKey();
        logKey( "write()", key );

        Path path = Paths.get( storageRoot, key );
        File dir = path.getParent().toFile();
        if ( !dir.isDirectory() && !dir.mkdirs() )
        {
            throw new RuntimeException( "Cannot create storage directory: " + dir );
        }

        try (ObjectOutputStream out = new ObjectOutputStream(
                new GZIPOutputStream( new FileOutputStream( path.toFile() ) ) ))
        {
            out.writeObject( new StorageFileEntry( entry ) );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Cannot store: " + path, e );
        }
    }

    @Override
    public boolean delete( Object key )
    {
        logKey( "delete()", key );
        Path path = Paths.get( storageRoot, key.toString() );
        File file = path.toFile();
        return file.exists() && file.delete();
    }

    @Override
    public MarshalledEntry<String, byte[]> load( Object key )
    {
        logKey( "load()", key );
        Path path = Paths.get( storageRoot, key.toString() );
        File file = path.toFile();
        if ( !file.exists() )
        {
            return null;
        }

        try (ObjectInputStream in = new ObjectInputStream( new GZIPInputStream( new FileInputStream( file ) ) ))
        {
            return (StorageFileEntry) in.readObject();
        }
        catch ( ClassNotFoundException | IOException e )
        {
            throw new RuntimeException( "Cannot load: " + key, e );
        }
    }

    private void logKey( String operation, Object key )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "{}: Key is of type: {}, value: '{}'", operation, key.getClass().getName(), key );
    }

    @Override
    public boolean contains( Object key )
    {
        logKey( "contains()", key );
        Path path = Paths.get( storageRoot, key.toString() );
        return path.toFile().exists();
    }

    @Override
    public void start()
    {
    }

    @Override
    public void stop()
    {
    }

    @Override
    public void process( KeyFilter<? super String> filter, CacheLoaderTask<String, byte[]> task, Executor executor,
                         boolean fetchValue, boolean fetchMetadata )
    {
        Path root = Paths.get( storageRoot );
        walkFiles( ( path -> {
            if ( filter.accept( path.relativize( root ).toString() ) )
            {
                executor.execute( () -> {
                    if ( Thread.currentThread().isInterrupted() )
                    {
                        Logger logger = LoggerFactory.getLogger( getClass() );
                        logger.debug( "The cache-loader process thread is interrupted at the start. Bailing out." );
                        return;
                    }

                    try (ObjectInputStream in = new ObjectInputStream(
                            new GZIPInputStream( new FileInputStream( path.toFile() ) ) ))
                    {
                        StorageFileEntry entry = new StorageFileEntry();
                        entry.load( in, fetchMetadata, fetchValue );

                        StorageFileTaskContext ctx = new StorageFileTaskContext();
                        task.processEntry( entry, ctx );
                    }
                    catch ( ClassNotFoundException | IOException e )
                    {
                        Logger logger = LoggerFactory.getLogger( getClass() );
                        logger.error( "Cannot read file: " + path, e );
                    }
                    catch ( InterruptedException e )
                    {
                        Thread.currentThread().interrupt();
                        Logger logger = LoggerFactory.getLogger( getClass() );
                        logger.debug( "cache-loader process method interrupted on: " + path, e );
                    }
                } );
            }
        } ) );
    }

    @Override
    public int size()
    {
        AtomicInteger counter = new AtomicInteger( 0 );
        walkFiles( ( p ) -> counter.incrementAndGet() );
        return counter.get();
    }

    @Override
    public void clear()
    {
        File root = Paths.get( storageRoot ).toFile();
        if ( !root.exists() || !root.isDirectory() || root.list().length < 1 )
        {
            return;
        }

        Optional<RuntimeException> e = Stream.of( root.list() ).map( ( named ) -> new File( root, named ) ).map( ( file)->{
            RuntimeException error = null;
            try
            {
                FileUtils.forceDelete( file );
            }
            catch ( IOException ioe )
            {
                error = new RuntimeException("Cannot delete: " + file, ioe );
            }
            return error;

        }).findFirst();

        if ( e.isPresent() )
        {
            throw e.get();
        }
    }

    @Override
    public void purge( Executor threadPool, PurgeListener<? super String> listener )
    {
        walkFiles( ( p ) -> {
            threadPool.execute( () -> {
                long expiration = -1;
                try (ObjectInputStream in = new ObjectInputStream(
                        new GZIPInputStream( new FileInputStream( p.toFile() ) ) ))
                {
                    expiration = StorageFileEntry.peekExpiryTime( in );
                }
                catch ( IOException e )
                {
                    Logger logger = LoggerFactory.getLogger( getClass() );
                    logger.error( "Cannot open file to peek at expiration time: " + p, e );
                }

                if ( System.currentTimeMillis() > expiration )
                {
                    p.toFile().delete();
                    listener.entryPurged( p.relativize( Paths.get( storageRoot ) ).toString() );
                }
            } );
        } );
    }

    private void walkFiles( Consumer<Path> operation )
    {
        walkFiles( Paths.get( storageRoot ), operation );
    }

    public void walkFiles( Path root, Consumer<Path> operation )
    {
        Stream.of( root.toFile().list() ).map( ( s -> root.resolve( s ) ) ).forEach( ( p ) -> {
            if ( p.toFile().isDirectory() )
            {
                walkFiles( p, operation );
            }
            else
            {
                operation.accept( p );
            }
        } );
    }
}
