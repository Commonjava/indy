/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.data.StoreEventDispatcher;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
@Alternative
public class DataFileStoreDataManager
    extends MemoryStoreDataManager
{
    public static final String INDY_STORE = "indy";

    public static final String LOAD_FROM_DISK = "load-from-disk";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private DataFileManager manager;

    @Inject
    private ObjectMapper serializer;

    private boolean started;

    protected DataFileStoreDataManager()
    {
    }

    public DataFileStoreDataManager( final DataFileManager manager, final ObjectMapper serializer,
                                        final StoreEventDispatcher dispatcher, IndyConfiguration config )
    {
        super( dispatcher, config );
        this.manager = manager;
        this.serializer = serializer;
    }

    @PostConstruct
    public void readDefinitions()
    {
        try
        {
            DataFile dir = manager.getDataFile( INDY_STORE, StoreType.hosted.singularEndpointName() );
            final ChangeSummary summary =
                new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                   "Reading definitions from disk, culling invalid definition files." );

            String[] files = dir.list();
            if ( files != null )
            {
                for ( final String file : files )
                {
                    final DataFile f = dir.getChild( file );
                    try
                    {
                        final String json = f.readString();
                        final HostedRepository h = serializer.readValue( json, HostedRepository.class );
                        if ( h == null )
                        {
                            f.delete( summary );
                        }
                        else
                        {
                            storeArtifactStore( h, summary, false, false,
                                                new EventMetadata().set( StoreDataManager.EVENT_ORIGIN, LOAD_FROM_DISK ) );
                        }
                    }
                    catch ( final IOException e )
                    {
                        logger.error( String.format( "Failed to load deploy point: %s. Reason: %s", f, e.getMessage() ),
                                      e );
                    }
                }
            }

            dir = manager.getDataFile( INDY_STORE, StoreType.remote.singularEndpointName() );
            files = dir.list();
            if ( files != null )
            {
                for ( final String file : files )
                {
                    final DataFile f = dir.getChild( file );
                    try
                    {
                        final String json = f.readString();
                        final RemoteRepository r = serializer.readValue( json, RemoteRepository.class );
                        if ( r == null )
                        {
                            f.delete( summary );
                        }
                        else
                        {
                            storeArtifactStore( r, summary, false, false,
                                                new EventMetadata().set( StoreDataManager.EVENT_ORIGIN, LOAD_FROM_DISK ) );
                        }
                    }
                    catch ( final IOException e )
                    {
                        logger.error( String.format( "Failed to load repository: %s. Reason: %s", f, e.getMessage() ),
                                      e );
                    }
                }
            }

            dir = manager.getDataFile( INDY_STORE, StoreType.group.singularEndpointName() );
            files = dir.list();
            if ( files != null )
            {
                for ( final String file : files )
                {
                    final DataFile f = dir.getChild( file );
                    try
                    {
                        final String json = f.readString();
                        final Group g = serializer.readValue( json, Group.class );
                        if ( g == null )
                        {
                            f.delete( summary );
                        }
                        else
                        {
                            storeArtifactStore( g, summary, false, false,
                                                new EventMetadata().set( StoreDataManager.EVENT_ORIGIN, LOAD_FROM_DISK ) );
                        }
                    }
                    catch ( final IOException e )
                    {
                        logger.error( String.format( "Failed to load group: %s. Reason: %s", f, e.getMessage() ), e );
                    }
                }
            }

            started = true;
        }
        catch ( final IndyDataException e )
        {
            throw new IllegalStateException( "Failed to start store data manager: " + e.getMessage(), e );
        }
    }

    private void store( final boolean skipIfExists, final ChangeSummary summary, final ArtifactStore... stores )
        throws IndyDataException
    {
        for ( final ArtifactStore store : stores )
        {
            final DataFile f =
                manager.getDataFile( INDY_STORE, store.getKey()
                                                       .getType()
                                                       .singularEndpointName(), store.getName() + ".json" );
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

    private void delete( final StoreType type, final String name, final ChangeSummary summary )
        throws IndyDataException
    {
        final DataFile f = manager.getDataFile( INDY_STORE, type.singularEndpointName(), name + ".json" );
        try
        {
            f.delete( summary );
        }
        catch ( final IOException e )
        {
            throw new IndyDataException( "Cannot delete store definition: {}:{} in file: {}. Reason: {}", e, type,
                                          name, f, e.getMessage() );
        }
    }

    @Override
    protected void postStore( final ArtifactStore store, final ChangeSummary summary, final boolean exists,
                              final boolean fireEvents, final EventMetadata eventMetadata )
        throws IndyDataException
    {
        store( false, summary, store );
        super.postStore( store, summary, exists, fireEvents, eventMetadata );
    }

    @Override
    protected void postDelete( final ArtifactStore store, final ChangeSummary summary, final boolean fireEvents,
                               final EventMetadata eventMetadata )
        throws IndyDataException
    {
        delete( store.getKey()
                     .getType(), store.getName(), summary );
        super.postDelete( store, summary, fireEvents, eventMetadata );
    }

    @Override
    public void clear( final ChangeSummary summary )
        throws IndyDataException
    {
        super.clear( summary );

        final DataFile basedir = manager.getDataFile( INDY_STORE );
        try
        {
            basedir.delete( summary );
        }
        catch ( final IOException e )
        {
            throw new IndyDataException( "Failed to delete Indy storage files: {}", e, e.getMessage() );
        }
    }

    @Override
    public void install()
        throws IndyDataException
    {
        if ( !manager.getDataFile( INDY_STORE )
                     .isDirectory() )
        {
            final ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "Initializing defaults" );

            storeArtifactStore( new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" ), summary,
                                true, false, new EventMetadata() );

            storeArtifactStore( new Group( "public", new StoreKey( StoreType.remote, "central" ) ), summary, true,
                                false, new EventMetadata() );
        }
    }

    @Override
    public void reload()
        throws IndyDataException
    {
        // NOTE: Call to super for this, because the local implementation DELETES THE DB DIR!!!
        super.clear( new ChangeSummary( ChangeSummary.SYSTEM_USER, "Reloading from storage" ) );
        readDefinitions();
    }

    public DataFile getDataFile( final StoreKey key )
    {
        return manager.getDataFile( INDY_STORE, key.getType()
                                                    .singularEndpointName(), key.getName() + ".json" );
    }

    public DataFileManager getFileManager()
    {
        return manager;
    }

    @Override
    public boolean isStarted()
    {
        return started && super.isStarted();
    }

}
