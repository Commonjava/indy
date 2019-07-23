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
package org.commonjava.indy.content.index;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.content.index.conf.ContentIndexConfig;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by jdcasey on 5/2/16.
 */
@Decorator
public abstract class IndexingDirectContentAccessDecorator
        implements DirectContentAccess
{

    @Inject
    private ContentIndexManager indexManager;

    @Inject
    @Delegate
    private DirectContentAccess delegate;

    @Inject
    private ContentIndexConfig indexCfg;

    @Override
    public Transfer retrieveRaw( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        Transfer transfer = getIndexedTransfer( store, path );
        if ( transfer != null )
        {
            return transfer;
        }
        else if ( indexCfg.isAuthoritativeIndex() && store.isAuthoritativeIndex() )
        {
            logger.debug(
                    "Not found indexed transfer: {} and authoritative index switched on. Considering not found and return null." );
            return null;
        }

        transfer = delegate.retrieveRaw( store, path, eventMetadata );

        if ( transfer != null )
        {
            logger.debug( "Got transfer from delegate: {} (will index)", transfer );

            indexManager.indexTransferIn( transfer, store.getKey() );
        }

        logger.debug( "Returning transfer: {}", transfer );
        return transfer;
    }

    @Override
    public List<Transfer> retrieveAllRaw( final List<? extends ArtifactStore> stores, final String path, final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        List<Transfer> results = new ArrayList<>();
        stores.stream().map( ( store ) -> {
            try
            {
                return retrieveRaw( store, path, eventMetadata );
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

    private Transfer getIndexedTransfer( ArtifactStore store, String path )
            throws IndyWorkflowException
    {
        StoreKey indexedStoreKey =
                indexManager.getIndexedStoreKey( store.getKey(), path );

        if ( indexedStoreKey != null )
        {
            Transfer transfer = delegate.getTransfer( store, path );
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
    public List<StoreResource> listRaw( ArtifactStore store, String parentPath )
            throws IndyWorkflowException
    {
        // TODO: Need to use the index, but I think we may need to combine with NFC for this to work.
//        List<IndexedStorePath> paths =
//                indexManager.lookupIndexedSubPathsByTopKey( store.getKey(), parentPath );


        List<StoreResource> raws = delegate.listRaw( store, parentPath );
        if ( indexCfg.isAuthoritativeIndex() && store.isAuthoritativeIndex() )
        {
            // Here we will filter the resources if authoritative index set on. Only these indexed resources will be return.
            return raws.stream()
                       .filter( res -> indexManager.getIndexedStoreKey( res.getStoreKey(), res.getPath() ) != null )
                       .collect( Collectors.toList() );
        }
        else
        {
            return raws;
        }
    }
}
