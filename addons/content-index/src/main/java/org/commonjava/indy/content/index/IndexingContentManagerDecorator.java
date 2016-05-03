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
package org.commonjava.indy.content.index;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Decorator for ContentManager which uses Infinispan to index content to avoid having to iterate all members of large
 * groups looking for a file.
 *
 * Created by jdcasey on 3/15/16.
 */
@Decorator
public abstract class IndexingContentManagerDecorator
        implements ContentManager
{
    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private SpecialPathManager specialPathManager;

    @Delegate
    @Any
    @Inject
    private ContentManager delegate;

    @Inject
    private ContentIndexManager indexManager;

    protected IndexingContentManagerDecorator()
    {
    }

    protected IndexingContentManagerDecorator( final ContentManager delegate,
                                               final StoreDataManager storeDataManager,
                                               final SpecialPathManager specialPathManager,
                                               final ContentIndexManager indexManager )
    {
        this.delegate = delegate;
        this.storeDataManager = storeDataManager;
        this.specialPathManager = specialPathManager;
        this.indexManager = indexManager;
    }

    @Override
    public Transfer retrieveFirst( final List<? extends ArtifactStore> stores, final String path )
            throws IndyWorkflowException
    {
        return retrieveFirst( stores, path, new EventMetadata() );
    }

    @Override
    public Transfer retrieveFirst( final List<? extends ArtifactStore> stores, final String path, final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Transfer transfer = null;
        for ( ArtifactStore store : stores )
        {
            transfer = retrieve( store, path, eventMetadata );
            if ( transfer != null )
            {
                break;
            }
        }

        return transfer;
    }

    @Override
    public List<Transfer> retrieveAll( final List<? extends ArtifactStore> stores, final String path )
            throws IndyWorkflowException
    {
        return retrieveAll( stores, path, new EventMetadata() );
    }

    @Override
    public List<Transfer> retrieveAll( final List<? extends ArtifactStore> stores, final String path, final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        List<Transfer> results = new ArrayList<>();
        stores.stream().map( ( store ) -> {
            try
            {
                return retrieve( store, path, eventMetadata );
            }
            catch ( IndyWorkflowException e )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.error(
                        String.format( "Failed to retrieve indexed content: %s:%s. Reason: %s", store.getKey(),
                                       path, e.getMessage() ), e );
            }

            return null;
        } ).filter((transfer)->transfer != null).forEachOrdered( ( transfer ) -> {
            if ( transfer != null )
            {
                results.add( transfer );
            }
        } );

        return results;
    }

    @Override
    public Transfer retrieve( final ArtifactStore store, final String path )
            throws IndyWorkflowException
    {
        return retrieve( store, path );
    }

    @Override
    public Transfer retrieve( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        Transfer transfer = getIndexedTransfer( store, path, TransferOperation.DOWNLOAD );
        if ( transfer != null )
        {
            return transfer;
        }

        if ( StoreType.group == store.getKey().getType() )
        {
            logger.debug( "No group index hits. Devolving to member store indexes." );

            KeyedLocation location = LocationUtils.toLocation( store );
            SpecialPathInfo specialPathInfo = specialPathManager.getSpecialPathInfo( location, path );
            if ( specialPathInfo == null || !specialPathInfo.isMergable() )
            {
                for ( StoreKey key : ( (Group) store ).getConstituents() )
                {
                    transfer = getIndexedMemberTransfer( key, store.getKey(), path );
                    if ( transfer != null )
                    {
                        return transfer;
                    }
                }
                logger.debug( "No index hits. Delegating to main content manager for: {} in: {}", path, store );
            }
            else
            {
                logger.debug( "Merged content. Delegating to main content manager for: {} in: {}", path, store );
                return delegate.retrieve( store, path, eventMetadata );
            }
        }

        transfer = delegate.retrieve( store, path, eventMetadata );

        if ( transfer != null )
        {
            logger.debug( "Got transfer from delegate: {} (will index)", transfer );

            indexManager.indexTransferIn( transfer, store.getKey(), LocationUtils.getKey( transfer ) );
        }

        logger.debug( "Returning transfer: {}", transfer );
        return transfer;
    }

    private Transfer getIndexedTransfer( ArtifactStore store, String path, TransferOperation op )
            throws IndyWorkflowException
    {
        IndexedStorePath storePath =
                indexManager.getIndexedStorePath( store.getKey(), path );

        if ( storePath != null )
        {
            Transfer transfer = delegate.getTransfer( store, path, op );
            if ( transfer == null || !transfer.exists() )
            {
                // something happened to the underlying Transfer...de-index it.
                indexManager.deIndexStorePath( store.getKey(), path );
            }
            else
            {
                return transfer;
            }
        }

        return null;
    }

    @Override
    public Transfer getTransfer( final ArtifactStore store, final String path, final TransferOperation op )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        Transfer transfer = getIndexedTransfer( store, path, TransferOperation.DOWNLOAD );
        if ( transfer != null )
        {
            return transfer;
        }

        if ( StoreType.group == store.getKey().getType() )
        {
            logger.debug( "No group index hits. Devolving to member store indexes." );
            for ( StoreKey key : ( (Group) store ).getConstituents() )
            {
                transfer = getIndexedMemberTransfer( key, store.getKey(), path );
                if ( transfer != null )
                {
                    return transfer;
                }
            }
        }

        transfer = delegate.getTransfer( store, path, op );
        if ( transfer != null )
        {
            indexManager.indexTransferIn( transfer, store.getKey(), LocationUtils.getKey( transfer ) );
        }

        return transfer;
    }

    private Transfer getIndexedMemberTransfer( StoreKey key, StoreKey topKey, String path )
            throws IndyWorkflowException
    {
        Transfer transfer = null;
        try
        {
            ArtifactStore memberStore = storeDataManager.getArtifactStore( key );

            transfer = getIndexedTransfer( memberStore, path, TransferOperation.DOWNLOAD );
            if ( transfer != null )
            {
                indexManager.indexTransferIn( transfer, topKey );
            }
        }
        catch ( IndyDataException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error(
                    String.format( "Cannot lookup ArtifactStore for key: %s. Reason: %s", key, e.getMessage() ),
                    e );
        }

        return transfer;
    }

    @Override
    public Transfer getTransfer( final StoreKey storeKey, final String path, final TransferOperation op )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        ArtifactStore store;
        try
        {
            store = storeDataManager.getArtifactStore( storeKey );
        }
        catch ( IndyDataException e )
        {
            throw new IndyWorkflowException(
                    String.format( "Cannot lookup ArtifactStore for key: %s. Reason: %s", storeKey, e.getMessage() ),
                    e );
        }

        Transfer transfer = getIndexedTransfer( store, path, TransferOperation.DOWNLOAD );
        if ( transfer != null )
        {
            return transfer;
        }

        if ( StoreType.group == storeKey.getType() )
        {
            logger.debug( "No group index hits. Devolving to member store indexes." );
            try
            {
                Group g = storeDataManager.getGroup( storeKey.getName() );

                for ( StoreKey key : g.getConstituents() )
                {
                    transfer = getIndexedMemberTransfer( key, store.getKey(), path );
                    if ( transfer != null )
                    {
                        return transfer;
                    }
                }
            }
            catch ( IndyDataException e )
            {
                logger.error(
                        String.format( "Cannot lookup ArtifactStore for key: %s. Reason: %s", storeKey, e.getMessage() ),
                        e );
            }
        }

        transfer = delegate.getTransfer( storeKey, path, op );
        if ( transfer != null )
        {
            indexManager.indexTransferIn( transfer, storeKey, LocationUtils.getKey( transfer ) );
        }

        return transfer;
    }

    @Override
    public Transfer getTransfer( final List<ArtifactStore> stores, final String path, final TransferOperation op )
            throws IndyWorkflowException
    {
        Transfer transfer = null;
        for ( ArtifactStore store : stores )
        {
            transfer = getTransfer( store, path, op );
            if ( transfer != null )
            {
                break;
            }
        }

        return transfer;
    }

    @Override
    public Transfer store( final ArtifactStore store, final String path, final InputStream stream, final TransferOperation op )
            throws IndyWorkflowException
    {
        return store( store, path, stream, op, new EventMetadata() );
    }

    @Override
    public Transfer store( final ArtifactStore store, final String path, final InputStream stream, final TransferOperation op,
                           final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Transfer transfer = delegate.store( store, path, stream, op, eventMetadata );
        if ( transfer != null )
        {
            indexManager.indexTransferIn( transfer, store.getKey(), LocationUtils.getKey( transfer ) );
        }

        return transfer;
    }

    @Override
    public Transfer store( final List<? extends ArtifactStore> stores, final String path, final InputStream stream, final TransferOperation op )
            throws IndyWorkflowException
    {
        return store( stores, path, stream, op, new EventMetadata() );
    }

    @Override
    public Transfer store( final List<? extends ArtifactStore> stores, final String path, final InputStream stream, final TransferOperation op,
                           final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Transfer transfer = delegate.store( stores, path, stream, op, eventMetadata );
        if ( transfer != null )
        {
            indexManager.indexTransferIn( transfer, LocationUtils.getKey( transfer ) );
        }

        return transfer;
    }

    @Override
    public boolean delete( final ArtifactStore store, final String path )
            throws IndyWorkflowException
    {
        return delete( store, path, new EventMetadata() );
    }

    @Override
    public boolean delete( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        boolean result = delegate.delete( store, path, eventMetadata );
        if ( result )
        {
            indexManager.deIndexStorePath( store.getKey(), path );
        }

        return result;
    }

    @Override
    public boolean deleteAll( final List<? extends ArtifactStore> stores, final String path )
            throws IndyWorkflowException
    {
        return deleteAll( stores, path, new EventMetadata() );
    }

    @Override
    public boolean deleteAll( final List<? extends ArtifactStore> stores, final String path, final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        boolean result = false;
        for ( ArtifactStore store : stores )
        {
            result = delete( store, path, eventMetadata ) || result;
        }

        return result;
    }

}
