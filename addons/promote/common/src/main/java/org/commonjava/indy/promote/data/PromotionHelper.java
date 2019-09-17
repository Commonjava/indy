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
package org.commonjava.indy.promote.data;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.promote.metrics.PathGauges;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.commonjava.indy.promote.validate.PromotionValidationException;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ApplicationScoped
public class PromotionHelper
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public PromotionHelper()
    {
    }

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private NotFoundCache nfc;

    @Inject
    private PathGauges pathGauges;

    @Inject
    private DownloadManager downloadManager;

    @Inject
    private ContentManager contentManager;

    public PromotionHelper( StoreDataManager storeManager, DownloadManager downloadManager,
                            ContentManager contentManager, NotFoundCache nfc )
    {
        this.storeManager = storeManager;
        this.downloadManager = downloadManager;
        this.contentManager = contentManager;
        this.nfc = nfc;
        this.pathGauges = new PathGauges();
    }

    /**
     * Clear NFC for the source store and affected groups.
     *
     * @param sourcePaths The set of paths that need to be cleared from the NFC.
     * @param store The store whose affected groups should have their NFC entries cleared
     */
    public void clearStoreNFC( final Set<String> sourcePaths, ArtifactStore store )
    {
        Set<String> paths = sourcePaths.stream()
                                       .map( sp -> sp.startsWith( "/" ) && sp.length() > 1 ? sp.substring( 1 ) : sp )
                                       .collect( Collectors.toSet() );

        paths.forEach( path -> {
            ConcreteResource resource = new ConcreteResource( LocationUtils.toLocation( store ), path );
            logger.trace( "Clearing NFC path: {} from: {}\n\tResource: {}", path, store.getKey(), resource );
            nfc.clearMissing( resource );
        } );

        Set<Group> groups;
        try
        {
            groups = storeManager.query().getGroupsAffectedBy( store.getKey() );
        }
        catch ( IndyDataException e )
        {
            logger.warn( "Failed to clear NFC for groups affected by " + store.getKey(), e );
            return;
        }

        if ( groups != null )
        {
            groups.forEach( group -> {
                KeyedLocation gl = LocationUtils.toLocation( group );
                paths.forEach( path -> {
                    ConcreteResource resource = new ConcreteResource( gl, path );
                    logger.trace( "Clearing NFC path: {} from: {}\n\tResource: {}", path, group.getKey(), resource );
                    nfc.clearMissing( resource );
                } );
            } );
        }
    }

    public void updatePathPromoteMetrics( int total, PathsPromoteResult result )
    {
        pathGauges.update( total, result );
    }

    public List<Transfer> getTransfersForPaths( final StoreKey source, final Set<String> paths )
                    throws IndyWorkflowException
    {
        final List<Transfer> contents = new ArrayList<>();
        for ( final String path : paths )
        {
            final Transfer transfer = downloadManager.getStorageReference( source, path );
            contents.add( transfer );
        }
        return contents;
    }

    public void purgeSourceQuietly( StoreKey src, Set<String> paths )
    {
        logger.debug( "Purge source, store: {}, paths: {}", src, paths );
        ArtifactStore store;
        try
        {
            store = storeManager.getArtifactStore( src );
        }
        catch ( IndyDataException e )
        {
            logger.warn( "Failed to purge source, store: " + src, e );
            return;
        }

        paths.forEach( p -> {
            try
            {
                contentManager.delete( store, p, new EventMetadata() );
            }
            catch ( IndyWorkflowException e )
            {
                logger.warn( "Failed to purge source (continue), store: " + src + ", path: " + p, e );
            }
        } );
    }

    public static void throwProperException( Exception e ) throws PromotionValidationException, IndyWorkflowException
    {
        if ( e instanceof PromotionValidationException )
        {
            throw (PromotionValidationException) e;
        }
        else if ( e instanceof IndyWorkflowException )
        {
            throw (IndyWorkflowException) e;
        }
        else
        {
            throw new IndyWorkflowException( "Promotion failed", e );
        }
    }

    /**
     * Rollback completed paths. If there is error thrown during deletion, report to caller via errors.
     */
    public List<String> deleteFromStore( final Set<String> completed, final ArtifactStore targetStore )
    {
        List<String> errors = new ArrayList<>();
        completed.forEach( p -> {
            try
            {
                contentManager.delete( targetStore, p );
            }
            catch ( IndyWorkflowException e )
            {
                String msg = "Failed to delete path " + p + " in " + targetStore.getKey();
                logger.error( msg, e );
                errors.add( msg );
            }
        } );
        return errors;
    }

    public boolean isRemoteTransfer( Transfer transfer )
    {
        Location loc = transfer.getLocation();
        if ( loc instanceof KeyedLocation )
        {
            return ((KeyedLocation) loc).getKey().getType() == StoreType.remote;
        }
        return false;
    }

    public Transfer redownload( Transfer transfer ) throws IndyWorkflowException
    {
        StoreKey key = ( (KeyedLocation) transfer.getLocation() ).getKey();
        try
        {
            return contentManager.retrieve( storeManager.getArtifactStore( key ), transfer.getPath() );
        }
        catch ( IndyDataException e )
        {
            logger.error( "Can not get store, key: {}", key );
        }
        return null;
    }

    // util class to hold repos check results
    class PromotionRepoRetrievalResult
    {
        final List<String> errors;
        final ArtifactStore targetStore, sourceStore;

        public PromotionRepoRetrievalResult( List<String> errors, ArtifactStore sourceStore, ArtifactStore targetStore )
        {
            this.errors = errors;
            this.targetStore = targetStore;
            this.sourceStore = sourceStore;
        }

        public boolean hasErrors()
        {
            return !errors.isEmpty();
        }
    }

    /**
     * Check whether the source and target repo exists.
     * @param request
     * @return errors
     */
    PromotionRepoRetrievalResult checkAndRetrieveSourceAndTargetRepos( PathsPromoteRequest request )
    {
        List<String> errors = new ArrayList<>();
        ArtifactStore sourceStore = null;
        ArtifactStore targetStore = null;

        try
        {
            sourceStore = storeManager.getArtifactStore( request.getSource() );
        }
        catch ( IndyDataException e )
        {
            String msg = String.format( "Failed to retrieve source store: %s. Reason: %s", request.getSource(),
                                        e.getMessage() );
            logger.error( msg, e );
            errors.add( msg );
        }

        try
        {
            targetStore = storeManager.getArtifactStore( request.getTarget() );
        }
        catch ( IndyDataException e )
        {
            String msg = String.format( "Failed to retrieve target store: %s. Reason: %s", request.getTarget(),
                                        e.getMessage() );
            logger.error( msg, e );
            errors.add( msg );
        }

        if ( targetStore == null || sourceStore == null )
        {
            String msg = String.format( "Failed to retrieve stores, source: %s, target: %s", request.getSource(),
                                        request.getTarget() );
            logger.error( msg );
            errors.add( msg );
        }

        return new PromotionRepoRetrievalResult( errors, sourceStore, targetStore );
    }

    public static long timeInSeconds( long begin )
    {
        return TimeUnit.MILLISECONDS.toSeconds( System.currentTimeMillis() - begin );
    }

    public static long timeInMillSeconds( long begin )
    {
        return System.currentTimeMillis() - begin;
    }
}
