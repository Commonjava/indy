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

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.data.StoreEventDispatcher;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.commonjava.indy.flat.data.DataFileStoreUtils.INDY_STORE;
import static org.commonjava.indy.flat.data.DataFileStoreUtils.LOAD_FROM_DISK;
import static org.commonjava.indy.flat.data.DataFileStoreUtils.deleteFromDisk;
import static org.commonjava.indy.flat.data.DataFileStoreUtils.loadFromDiskAnd;
import static org.commonjava.indy.flat.data.DataFileStoreUtils.storeToDisk;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;

@ApplicationScoped
@Alternative
public class DataFileStoreDataManager
    extends MemoryStoreDataManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private DataFileManager manager;

    @Inject
    private IndyObjectMapper serializer;

    private boolean started;

    protected DataFileStoreDataManager()
    {
    }

    public DataFileStoreDataManager( final DataFileManager manager, final IndyObjectMapper serializer,
                                        final StoreEventDispatcher dispatcher )
    {
        super( dispatcher );
        this.manager = manager;
        this.serializer = serializer;
        this.started = true;
    }

    @PostConstruct
    public void readDefinitions()
    {
        ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                   "Reading definitions from disk, culling invalid definition files." );

        loadFromDiskAnd( manager, serializer, summary, ( store ) -> {
            try
            {
                storeArtifactStore( store, summary, false, false,
                                    new EventMetadata().set( StoreDataManager.EVENT_ORIGIN, LOAD_FROM_DISK ) );
            }
            catch ( IndyDataException e )
            {
                throw new IllegalStateException( "Failed to start store data manager: " + e.getMessage(), e );
            }
        } );
        started = true;
    }

    @Override
    protected void postStore( final ArtifactStore store, ArtifactStore original, final ChangeSummary summary, final boolean exists,
                              final boolean fireEvents, final EventMetadata eventMetadata )
        throws IndyDataException
    {
        storeToDisk( manager, serializer, false, summary, store );
        super.postStore( store, original, summary, exists, fireEvents, eventMetadata );
    }

    @Override
    protected void postDelete( final ArtifactStore store, final ChangeSummary summary, final boolean fireEvents,
                               final EventMetadata eventMetadata )
        throws IndyDataException
    {
        deleteFromDisk( manager, store, summary );
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

            storeArtifactStore(
                    new RemoteRepository( MAVEN_PKG_KEY, "central", "http://repo.maven.apache.org/maven2/" ), summary,
                    true, false, new EventMetadata() );

            storeArtifactStore( new Group( MAVEN_PKG_KEY, "public", new StoreKey( StoreType.remote, "central" ) ),
                                summary, true, false, new EventMetadata() );
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
        return manager.getDataFile( INDY_STORE, key.getType().singularEndpointName(), key.getName() + ".json" );
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
