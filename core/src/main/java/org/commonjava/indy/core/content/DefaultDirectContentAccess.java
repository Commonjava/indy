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
package org.commonjava.indy.core.content;

import org.commonjava.cdi.util.weft.DrainingExecutorCompletionService;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftExecutorService;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.commonjava.indy.core.ctl.PoolUtils.detectOverloadVoid;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;

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
    @ExecutorConfig( named = "direct-content-access", threads = 8, priority = 8, maxLoadFactor = 100, loadSensitive = ExecutorConfig.BooleanLiteral.TRUE )
    private WeftExecutorService contentAccessService;


    public DefaultDirectContentAccess(){}

    public DefaultDirectContentAccess( final DownloadManager downloadManager, WeftExecutorService executorService )
    {
        this.downloadManager = downloadManager;
        this.contentAccessService = executorService;
    }


    @Override
    public List<Transfer> retrieveAllRaw( final List<? extends ArtifactStore> stores, final String path,
                                          final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        Map<ArtifactStore, Future<Transfer>> futures = new HashMap<>();

        detectOverloadVoid( () -> {
            for ( final ArtifactStore store : stores )
            {
                logger.trace( "Requesting retrieval of {} in {}", path, store );

                Future<Transfer> future = contentAccessService.submit( () -> {
                    logger.trace( "Retrieving {} in {}", path, store );
                    Transfer txfr = retrieveRaw( store, path, eventMetadata );
                    logger.trace( "Transfer {} in {} retrieved", path, store );
                    return txfr;
                } );

                futures.put( store, future );
            }
        } );

        final List<Transfer> txfrs = new ArrayList<>( stores.size() );
        for ( ArtifactStore store : stores )
        {
            Transfer txfr;
            try
            {
                logger.trace( "Waiting for transfer of {} in {}", path, store );
                Future<Transfer> future = futures.get( store );
                txfr = future.get();
                logger.trace( "Transfer {} in {} retrieved", path, store );
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
    public Transfer retrieveRaw( final ArtifactStore store, String path, final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        // npm should handle the path as '/project' not '/project/package.json' when retrieves a remote registry
        if ( store.getType() == remote && store.getPackageType().equals( NPM_PKG_KEY ) )
        {
            String project = path.substring( 0, path.length()-13 );
            if ( project != null && project.length() > 0 )
            {
                path = project;
            }
        }

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Attempting to retrieve: {} from: {}", path, store.getKey() );

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
    public List<StoreResource> listRaw( ArtifactStore store, String path, EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        return downloadManager.list( store, path, eventMetadata );
    }

    @Override
    public Map<String, List<StoreResource>> listRaw( ArtifactStore store, List<String> parentPathList )
            throws IndyWorkflowException
    {
        return listRaw( store, parentPathList, new EventMetadata() );
    }

    private static final class StoreListingResult
    {
        private String path;
        private List<StoreResource> listing;

        StoreListingResult( final String path, final List<StoreResource> listing )
        {
            this.path = path;
            this.listing = listing;
        }
    }

    @Override
    public Map<String, List<StoreResource>> listRaw( ArtifactStore store,
                                                     List<String> parentPathList, EventMetadata eventMetadata ) throws IndyWorkflowException
    {
        DrainingExecutorCompletionService<StoreListingResult> svc =
                new DrainingExecutorCompletionService<>( contentAccessService );

        Logger logger = LoggerFactory.getLogger( getClass() );
        detectOverloadVoid( ()->{
            for ( final String path : parentPathList )
            {
                logger.trace( "Requesting listing of {} in {}", path, store );
                svc.submit( ()->{
                    logger.trace( "Starting listing of {} in {}", path, store );
                    List<StoreResource> listRaw = listRaw( store, path, eventMetadata );
                    logger.trace( "Listing of {} in {} finished", path, store );
                    return new StoreListingResult( path, listRaw );
                });
            }
        } );

        final Map<String, List<StoreResource>> result = new HashMap<>();
        try
        {
            svc.drain( slr -> result.put( slr.path, slr.listing ) );
        }
        catch ( InterruptedException ex )
        {
            throw new IndyWorkflowException( "Listing retrieval in %s was interrupted", ex, store );
        }
        catch ( ExecutionException ex )
        {
            throw new IndyWorkflowException( "There was an error in listing retrieval for %s: %s", ex,
                                             store, ex );
        }

        return result;
    }

}
