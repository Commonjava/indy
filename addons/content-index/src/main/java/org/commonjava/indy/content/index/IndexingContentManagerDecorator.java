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
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
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

    @Inject
    private NotFoundCache nfc;

    protected IndexingContentManagerDecorator()
    {
    }

    protected IndexingContentManagerDecorator( final ContentManager delegate, final StoreDataManager storeDataManager,
                                               final SpecialPathManager specialPathManager,
                                               final ContentIndexManager indexManager, final NotFoundCache nfc )
    {
        this.delegate = delegate;
        this.storeDataManager = storeDataManager;
        this.specialPathManager = specialPathManager;
        this.indexManager = indexManager;
        this.nfc = nfc;
    }

    @Override
    public Transfer retrieveFirst( final List<? extends ArtifactStore> stores, final String path )
            throws IndyWorkflowException
    {
        return retrieveFirst( stores, path, new EventMetadata() );
    }

    @Override
    public Transfer retrieveFirst( final List<? extends ArtifactStore> stores, final String path,
                                   final EventMetadata eventMetadata )
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
    public List<Transfer> retrieveAll( final List<? extends ArtifactStore> stores, final String path,
                                       final EventMetadata eventMetadata )
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
                        String.format( "Failed to retrieve indexed content: %s:%s. Reason: %s", store.getKey(), path,
                                       e.getMessage() ), e );
            }

            return null;
        } ).filter( ( transfer ) -> transfer != null ).forEachOrdered( ( transfer ) -> {
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

        Transfer transfer = getIndexedTransfer( store.getKey(), null, path, TransferOperation.DOWNLOAD );
        if ( transfer != null )
        {
            return transfer;
        }

        StoreType type = store.getKey().getType();
//        if ( StoreType.hosted == type )
//        {
//            // hosted repos are completely indexed, since the store() method maintains the index
//            // So, if it wasn't found in the index (above), and we're looking at a hosted repo, it's not here.
//            logger.debug( "HOSTED / Not-Indexed: {}/{}", store.getKey(), path );
//            return null;
//        }
//        else if ( StoreType.group == type )
        if ( StoreType.group == type )
        {
            ConcreteResource resource = new ConcreteResource( LocationUtils.toLocation( store ), path );
            if ( nfc.isMissing( resource ) )
            {
                return null;
            }

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
                        nfc.clearMissing( resource );
                        return transfer;
                    }
                }
                logger.debug( "No index hits. Delegating to main content manager for: {} in: {}", path, store );
            }
            else
            {
                logger.debug( "Merged content. Delegating to main content manager for: {} in: {}", path, store );
                transfer = delegate.retrieve( store, path, eventMetadata );
                if ( transfer == null )
                {
                    nfc.addMissing( resource );
                }

                return transfer;
            }
        }

        transfer = delegate.retrieve( store, path, eventMetadata );

        if ( transfer != null )
        {
            logger.debug( "Got transfer from delegate: {} (will index)", transfer );

            indexManager.indexTransferIn( transfer, store.getKey() );
        }

        logger.debug( "Returning transfer: {}", transfer );
        return transfer;
    }

    private Transfer getIndexedTransfer( final StoreKey storeKey, final StoreKey topKey, final String path, final TransferOperation op )
            throws IndyWorkflowException
    {
        IndexedStorePath storePath = indexManager.getIndexedStorePath( storeKey, path );

        if ( storePath != null )
        {
            Transfer transfer = delegate.getTransfer( storeKey, path, op );
            if ( transfer == null || !transfer.exists() )
            {
                // something happened to the underlying Transfer...de-index it, and don't return it.
                indexManager.deIndexStorePath( storeKey, path );
                if ( topKey != null )
                {
                    indexManager.deIndexStorePath( topKey, path );
                }
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

        Transfer transfer = getIndexedTransfer( store.getKey(), null, path, TransferOperation.DOWNLOAD );
        if ( transfer != null )
        {
            return transfer;
        }

        ConcreteResource resource = new ConcreteResource( LocationUtils.toLocation( store ), path );
        StoreType type = store.getKey().getType();

        if ( StoreType.group == type )
        {
            if ( !nfc.isMissing( resource ) )
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
        }

        transfer = delegate.getTransfer( store, path, op );
        // index the transfer only if it exists, it cannot be null at this point
        if ( transfer.exists() )
        {
            indexManager.indexTransferIn( transfer, store.getKey() );
        }
        else
        {
            nfc.addMissing( resource );
        }

        return transfer;
    }

    private Transfer getIndexedMemberTransfer( final StoreKey key, final StoreKey topKey, final String path )
            throws IndyWorkflowException
    {
        Transfer transfer;
        transfer = getIndexedTransfer( key, topKey, path, TransferOperation.DOWNLOAD );
        if ( transfer != null )
        {
            indexManager.indexTransferIn( transfer, key, topKey );
        }
        else if ( StoreType.group == key.getType() )
        {
            try
            {
                Group g = storeDataManager.getGroup( key.getName() );
                if ( g != null )
                {
                    for ( StoreKey memberKey : g.getConstituents() )
                    {
                        transfer = getIndexedMemberTransfer( memberKey, topKey, path );
                        if ( transfer != null )
                        {
                            // the other keys' index will be added in the recursive call...but we need to add the intermediary here.
                            indexManager.indexTransferIn( transfer, key );
                            return transfer;
                        }
                    }
                }
            }
            catch ( IndyDataException e )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.error(
                        String.format( "Failed to lookup group: %s (in membership closure of: %s). Reason: %s", key,
                                       topKey, e.getMessage() ), e );
            }
        }

        return transfer;
    }

    @Override
    public Transfer getTransfer( final StoreKey storeKey, final String path, final TransferOperation op )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        Transfer transfer = getIndexedTransfer( storeKey, null, path, TransferOperation.DOWNLOAD );
        if ( transfer != null )
        {
            logger.debug( "Returning indexed transfer: {}", transfer );
            return transfer;
        }

        ArtifactStore store;
        try
        {
            store = storeDataManager.getGroup( storeKey.getName() );
        }
        catch ( IndyDataException e )
        {
            throw new IndyWorkflowException( "Failed to lookup ArtifactStore: %s for NFC handling. Reason: %s", e,
                                             storeKey, e.getMessage() );
        }

        ConcreteResource resource = new ConcreteResource( LocationUtils.toLocation( store ), path );
        StoreType type = storeKey.getType();

        if ( StoreType.group == type )
        {
            Group g= (Group) store;

            if ( g == null )
            {
                throw new IndyWorkflowException( "Cannot find requested group: %s", storeKey );
            }

            if ( nfc.isMissing( resource ) )
            {
                logger.debug( "NFC / MISSING: {}", resource );
                return null;
            }

            logger.debug( "No group index hits. Devolving to member store indexes." );
            for ( StoreKey key : g.getConstituents() )
            {
                transfer = getIndexedMemberTransfer( key, storeKey, path );
                if ( transfer != null )
                {
                    logger.debug( "Returning indexed transfer: {} from member: {}", transfer, key );
                    return transfer;
                }
            }
        }

        transfer = delegate.getTransfer( storeKey, path, op );
        if ( transfer != null )
        {
            logger.debug( "Indexing transfer: {}", transfer );
            indexManager.indexTransferIn( transfer, storeKey );
        }
        else
        {
            nfc.addMissing( resource );
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
    public Transfer store( final ArtifactStore store, final String path, final InputStream stream,
                           final TransferOperation op )
            throws IndyWorkflowException
    {
        return store( store, path, stream, op, new EventMetadata() );
    }

    @Override
    public Transfer store( final ArtifactStore store, final String path, final InputStream stream,
                           final TransferOperation op, final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Storing: {} in: {}", path, store.getKey() );
        Transfer transfer = delegate.store( store, path, stream, op, eventMetadata );
        if ( transfer != null )
        {
            logger.trace( "Indexing: {} in: {}", transfer, store.getKey() );
            indexManager.indexTransferIn( transfer, store.getKey() );

            if ( store instanceof Group )
            {
                nfc.clearMissing( new ConcreteResource( LocationUtils.toLocation( store ), path ) );
            }
        }

        return transfer;
    }

    //    @Override
    //    public Transfer store( final List<? extends ArtifactStore> stores, final String path, final InputStream stream, final TransferOperation op )
    //            throws IndyWorkflowException
    //    {
    //        return store( stores, path, stream, op, new EventMetadata() );
    //    }

    @Override
    public Transfer store( final List<? extends ArtifactStore> stores, final StoreKey topKey, final String path,
                           final InputStream stream, final TransferOperation op, final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Transfer transfer = delegate.store( stores, topKey, path, stream, op, eventMetadata );
        if ( transfer != null )
        {
            indexManager.indexTransferIn( transfer, topKey );

            try
            {
                ArtifactStore topStore = storeDataManager.getArtifactStore( topKey );
                nfc.clearMissing( new ConcreteResource( LocationUtils.toLocation( topStore ), path ) );
            }
            catch ( IndyDataException e )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.error( String.format( "Failed to retrieve top store: %s for NFC management. Reason: %s",
                                             topKey, e.getMessage()), e );
            }
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
    public boolean deleteAll( final List<? extends ArtifactStore> stores, final String path,
                              final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        boolean result = false;
        for ( ArtifactStore store : stores )
        {
            result = delete( store, path, eventMetadata ) | result;
        }

        return result;
    }

}
