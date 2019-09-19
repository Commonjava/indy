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
package org.commonjava.indy.flat.data;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class DataFileStoreUtils
{
    private static final Logger logger = LoggerFactory.getLogger( DataFileStoreUtils.class );

    public static final String INDY_STORE = "indy";

    public static final String LOAD_FROM_DISK = "load-from-disk";

    /**
     * Load all store definitions from disk and apply consumer function.
     */
    public static void loadFromDiskAnd( DataFileManager manager, IndyObjectMapper serializer,
                                        final ChangeSummary summary, Consumer<ArtifactStore> consumer )
    {
        loadFromDiskAnd( manager, serializer, null, summary, consumer );
    }

    /**
     * Load store definitions from disk and apply consumer function.
     * @param manager
     * @param serializer
     * @param key if null, load all.
     * @param summary
     * @param consumer
     */
    public static void loadFromDiskAnd( DataFileManager manager, IndyObjectMapper serializer, StoreKey key,
                                        final ChangeSummary summary, Consumer<ArtifactStore> consumer )
    {
        if ( key != null ) // Load a single file
        {
            DataFile f = manager.getDataFile( INDY_STORE, key.getPackageType(), key.getType().singularEndpointName(),
                                              key.getName() + ".json" );
            if ( f.exists() )
            {
                ArtifactStore store;
                try
                {
                    String json = f.readString();
                    store = serializer.readValue( json, key.getType().getStoreClass() );
                }
                catch ( IOException e )
                {
                    logger.error( "Failed to read file", e );
                    return;
                }
                consumer.accept( store );
            }
            return;
        }

        // Load all
        DataFile[] packageDirs = manager.getDataFile( INDY_STORE ).listFiles( ( f ) -> true );
        for ( DataFile pkgDir : packageDirs )
        {
            for ( StoreType type : StoreType.values() )
            {
                DataFile[] files = pkgDir.getChild( type.singularEndpointName() ).listFiles( f -> true );
                if ( files != null )
                {
                    for ( final DataFile f : files )
                    {
                        try
                        {
                            final String json = f.readString();
                            final ArtifactStore store = serializer.readValue( json, type.getStoreClass() );
                            if ( store == null )
                            {
                                f.delete( summary );
                            }
                            else
                            {
                                consumer.accept( store );
                            }
                        }
                        catch ( final IOException e )
                        {
                            logger.error( String.format( "Failed to load %s store: %s. Reason: %s", type, f,
                                                         e.getMessage() ), e );
                            try
                            {
                                f.delete( summary );
                            }
                            catch ( IOException e1 )
                            {
                                logger.error( "Failed to delete invalid store definition file: " + f, e );
                            }
                        }
                    }
                }
            }
        }
    }

    public static void storeToDisk( final DataFileManager manager, final IndyObjectMapper serializer,
                                    final boolean skipIfExists, final ChangeSummary summary,
                                    final ArtifactStore... stores ) throws IndyDataException
    {
        for ( final ArtifactStore store : stores )
        {
            final DataFile f = manager.getDataFile( INDY_STORE, store.getPackageType(),
                                                    store.getType().singularEndpointName(), store.getName() + ".json" );

            if ( skipIfExists && f.exists() )
            {
                continue;
            }

            final DataFile d = f.getParent();
            if ( !d.mkdirs() )
            {
                throw new IndyDataException( "Cannot create storage directory: {} for definition: {}", d, store );
            }

            try
            {
                final String json = serializer.writeValueAsString( store );
                f.writeString( json, "UTF-8", summary );
                logger.debug( "Persisted {} to disk at: {}\n{}", store, f, json );
            }
            catch ( final IOException e )
            {
                throw new IndyDataException( "Cannot write definition: {} to: {}. Reason: {}", e, store, f,
                                             e.getMessage() );
            }
        }
    }

    public static void deleteFromDisk( final DataFileManager manager, final ArtifactStore store,
                                       final ChangeSummary summary ) throws IndyDataException
    {
        logger.trace( "Attempting to delete data file for store: {}", store.getKey() );

        final DataFile f =
                        manager.getDataFile( INDY_STORE, store.getPackageType(), store.getType().singularEndpointName(),
                                             store.getName() + ".json" );

        try
        {
            logger.trace( "Deleting file: {}", f );
            f.delete( summary );
        }
        catch ( final IOException e )
        {
            throw new IndyDataException( "Cannot delete store definition: {} in file: {}. Reason: {}", e,
                                         store.getKey(), f, e.getMessage() );
        }
    }

}
