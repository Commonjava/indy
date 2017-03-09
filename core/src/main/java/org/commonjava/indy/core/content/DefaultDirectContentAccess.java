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
package org.commonjava.indy.core.content;

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;

/**
 * Created by jdcasey on 5/2/16.
 */
public class DefaultDirectContentAccess
        implements DirectContentAccess
{

    @Inject
    private DownloadManager downloadManager;

    @Inject
    @WeftManaged
    @ExecutorConfig( named = "direct-content-access" )
    private ExecutorService executorService;


    public DefaultDirectContentAccess(){}

    public DefaultDirectContentAccess( final DownloadManager downloadManager )
    {
        this.downloadManager = downloadManager;
    }


    @Override
    public List<Transfer> retrieveAllRaw( final List<? extends ArtifactStore> stores, final String path,
                                          final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        CompletionService<Transfer> executor = new ExecutorCompletionService<>( executorService );
        for ( final ArtifactStore store : stores )
        {
            logger.debug( "Requesting retrieval of {} in {}", path, store );
            executor.submit( new Callable<Transfer>()
            {
                @Override
                public Transfer call() throws IndyWorkflowException
                {
                    logger.trace( "Retrieving {} in {}", path, store );
                    Transfer txfr = retrieveRaw( store, path, eventMetadata );
                    logger.trace( "Transfer {} in {} retrieved", path, store );
                    return txfr;
                }
            });
        }

        final List<Transfer> txfrs = new ArrayList<>( stores.size() );
        for ( ArtifactStore store : stores )
        {
            Transfer txfr;
            try
            {
                logger.trace( "Waiting for transfer of {} in {}", path, store );
                txfr = executor.take().get();
                logger.debug( "Transfer {} in {} retrieved", path, store );
            }
            catch ( InterruptedException ex )
            {
                throw new IndyWorkflowException( "Retrieval of %s in %s was interrupted", ex, path, store );
            }
            catch ( ExecutionException ex )
            {
                throw new IndyWorkflowException( "Error retrieving %s from %s: %s", ex, path, store, ex );
            }

            if ( txfr != null )
            {
                txfrs.add( txfr );
            }
        }

        return txfrs;
    }

    @Override
    public Transfer retrieveRaw( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "Attempting to retrieve: {} from: {}", path, store.getKey() );

        Transfer item = null;
        try
        {
            item = downloadManager.retrieve( store, path, eventMetadata );
        }
        catch ( IndyWorkflowException e )
        {
            e.filterLocationErrors();
        }

        return item;
    }

    @Override
    public Transfer getTransfer( final ArtifactStore store, final String path )
            throws IndyWorkflowException
    {
        return downloadManager.getStorageReference( store, path );
    }

    @Override
    public Transfer getTransfer( final StoreKey key, final String path )
            throws IndyWorkflowException
    {
        return downloadManager.getStorageReference( key, path );
    }

    @Override
    public boolean exists( ArtifactStore store, String path )
            throws IndyWorkflowException
    {
        return downloadManager.exists( store, path );
    }

    @Override
    public List<StoreResource> listRaw( ArtifactStore store, String path )
            throws IndyWorkflowException
    {
        return downloadManager.list( store, path );
    }

    @Override
    public Map<String, List<StoreResource>> listRaw( ArtifactStore store,
                                                     List<String> parentPathList ) throws IndyWorkflowException
    {
        CompletionService<List<StoreResource>> executor = new ExecutorCompletionService<>( executorService );
        Logger logger = LoggerFactory.getLogger( getClass() );
        for ( final String path : parentPathList )
        {
            logger.debug( "Requesting listing of {} in {}", path, store );
            executor.submit( new Callable<List<StoreResource>>()
            {
                @Override
                public List<StoreResource> call() throws IndyWorkflowException
                {
                    logger.trace( "Starting listing of {} in {}", path, store );
                    List<StoreResource> listRaw = listRaw( store, path );
                    logger.trace( "Listing of {} in {} finished", path, store );
                    return listRaw;
                }
            });
        }

        final Map<String, List<StoreResource>> result = new HashMap<>();
        for ( String path : parentPathList )
        {
            try
            {
                logger.trace( "Waiting for listing of {} in {}", path, store );
                List<StoreResource> listing = executor.take().get();
                logger.debug( "Listing of {} in {} received", path, store );
                if ( listing != null )
                {
                    result.put( path, listing );
                }
            }
            catch ( InterruptedException ex )
            {
                throw new IndyWorkflowException( "Listing retrieval of %s in %s was interrupted", ex, path, store );
            }
            catch ( ExecutionException ex )
            {
                throw new IndyWorkflowException( "There was an error in listing retrieval of %s in %s: %s", ex, path,
                                                 store, ex );
            }
        }

        return result;
    }

}
