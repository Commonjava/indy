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
package org.commonjava.indy.infinispan.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@Named( "infinispan-store-data-migration" )
public class InfinispanStoreDataMigrationAction
                implements MigrationAction
{

    private static final String STORE_DATA_V1_CACHE = "store-data";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private DataFileManager dataFileManager;

    @Inject
    private CacheProducer cacheProducer;

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private IndyObjectMapper objectMapper;

    protected InfinispanStoreDataMigrationAction()
    {
    }

    public InfinispanStoreDataMigrationAction( final DataFileManager dataFileManager,
                                               final InfinispanStoreDataManager infinispanStoreDataManager,
                                               final IndyObjectMapper mapper, final CacheProducer cacheProducer )
    {
        this.dataFileManager = dataFileManager;
        storeDataManager = infinispanStoreDataManager;
        objectMapper = mapper;
        this.cacheProducer = cacheProducer;
    }

    @Override
    public String getId()
    {
        return "Flat db to ISPN store-definition data migrator";
    }

    @Override
    public boolean migrate() throws IndyLifecycleException
    {
        String clsName = storeDataManager.getClass().getName();

        if ( !( storeDataManager instanceof InfinispanStoreDataManager ) )
        {
            logger.info( "Store manager {} is not based on Infinispan. Skipping migration.", clsName );
            return false;
        }

        if ( !storeDataManager.isEmpty() )
        {
            logger.info( "Store manager {} is not empty. Migration already done.", clsName );
            return true;
        }

        logger.info( "Starting store manager {} migration.", clsName );
        return doMigrate();
    }

    private boolean doMigrate()
    {
        ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "Migrate definitions from disk." );

        final Set<String> result = Collections.synchronizedSet( new HashSet<>() );

        loadFromLegacyCacheAnd( store -> {
            logger.info( "Migrating from legacy cache: {}", store.getKey() );

            ( (InfinispanStoreDataManager) storeDataManager ).putArtifactStoreInternal( store.getKey(), store );

            result.add( store.getKey().toString() );
        });

        if ( result.isEmpty() )
        {
            loadFromDiskAnd( dataFileManager, objectMapper, summary, ( store ) -> {
                logger.info( "Migrating from disk: {}", store.getKey() );

                ( (InfinispanStoreDataManager) storeDataManager ).putArtifactStoreInternal( store.getKey(), store );

                result.add( store.getKey().toString() );
            } );
        }

        logger.info( "Store manager migration done. Result: {}", result.size() );
        if ( logger.isDebugEnabled() )
        {
            result.forEach( ( s ) -> logger.debug( s ) );
        }

        return !result.isEmpty();
    }

    private void loadFromLegacyCacheAnd( final Consumer<ArtifactStore> storeConsumer )
    {
        if ( cacheProducer != null )
        {
            EmbeddedCacheManager cacheManager = cacheProducer.getCacheManager();
            if ( cacheManager.cacheExists( STORE_DATA_V1_CACHE ) )
            {
                logger.info( "Migrating from legacy store-data cache: {}", STORE_DATA_V1_CACHE );

                cacheProducer.getCache( STORE_DATA_V1_CACHE ).executeCache( c->{
                    c.entrySet().forEach( entry-> {
                        StoreKey key = (StoreKey) entry.getKey();
                        try
                        {
                            ArtifactStore store = objectMapper.readValue( (String) entry.getValue(), key.getType().getStoreClass() );
                            storeConsumer.accept( store );
                        }
                        catch ( JsonProcessingException e )
                        {
                            logger.error( "Failed to read store definition from legacy cache: " + key, e );
                        }
                    } );

                    c.clearAsync();

                    return null;
                } );
            }
        }
    }

    @Override
    public int getMigrationPriority()
    {
        return 99;
    }

    private static final String INDY_STORE = "indy";

    /**
     * Copy from DataFileStoreUtils. This is one-off action.
     */
    private void loadFromDiskAnd( DataFileManager manager, IndyObjectMapper serializer, final ChangeSummary summary,
                                  Consumer<ArtifactStore> consumer )
    {
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

}
