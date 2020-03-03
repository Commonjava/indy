/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.pkg.maven.change;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.core.change.event.IndyFileEventManager;
import org.commonjava.indy.core.content.group.GroupMergeHelper;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.pkg.maven.content.MetadataCacheManager;
import org.commonjava.indy.pkg.maven.content.group.MavenMetadataMerger;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.event.FileDeletionEvent;
import org.commonjava.maven.galley.event.FileEvent;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Set;

import static org.commonjava.indy.data.StoreDataManager.TARGET_STORE;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.pkg.maven.content.MetadataUtil.getMetadataPath;
import static org.commonjava.indy.util.LocationUtils.getKey;

@ApplicationScoped
public class MetadataMergePomChangeListener
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private DownloadManager fileManager;

    @Inject
    private IndyFileEventManager fileEvent;

    @Inject
    private MetadataCacheManager cacheManager;

    /**
     * this listener observes {@link org.commonjava.maven.galley.event.FileStorageEvent}
     * for a pom file, which means maven-metadata.xml will be cleared
     * when a version (pom) is uploaded.
     */
    public void onPomStorageEvent( @Observes final FileStorageEvent event )
    {
        metaClear( event, "updated" );
    }

    /**
     * this listener observes {@link org.commonjava.maven.galley.event.FileDeletionEvent}
     * for a pom file, which means maven-metadata.xml will be cleared
     * when a version (pom) is removed.
     */
    public void onPomDeletionEvent( @Observes final FileDeletionEvent event )
    {
        metaClear( event, "deleted" );
    }

    private void metaClear( final FileEvent event, final String eventOps )
    {
        final String path = event.getTransfer().getPath();

        if ( !path.endsWith( ".pom" ) )
        {
            return;
        }

        EventMetadata eventMetadata = event.getEventMetadata();

        final StoreKey key = getKey( event );
        final String clearPath = getMetadataPath( path );
        logger.info( "Pom file {} {}, will clean its matched metadata file {}, eventMetadata: {}", path, eventOps,
                     clearPath, eventMetadata );
        try
        {
            if ( hosted == key.getType() )
            {
                ArtifactStore hosted = null;
                if ( eventMetadata != null )
                {
                    hosted = (ArtifactStore) eventMetadata.get( TARGET_STORE );
                }
                if ( hosted == null )
                {
                    hosted = dataManager.getArtifactStore( key );
                }

                if ( doClear( hosted, clearPath ) )
                {
                    cacheManager.remove( key, clearPath );
                    logger.info( "Metadata file {} in store {} cleared.", clearPath, key );
                }

                final Set<Group> groups = dataManager.affectedBy( Arrays.asList( key ), event.getEventMetadata() );

                if ( groups != null )
                {
                    long begin = System.currentTimeMillis();
                    for ( final Group group : groups )
                    {
                        if ( doClear( group, clearPath ) )
                        {
                            cacheManager.remove( group.getKey(), clearPath );
                        }
                    }
                    logger.info( "Clearing metadata file {} for {} groups affected by {}, timeMillis: {}", clearPath,
                                 groups.size(), key, ( System.currentTimeMillis() - begin ) );
                }
            }
        }
        catch ( final IndyDataException e )
        {
            logger.warn( "Failed to regenerate maven-metadata.xml for artifacts after deployment to: {}"
                                 + "\nCannot retrieve associated groups: {}", key, e.getMessage() );
        }
    }

    private boolean doClear( final ArtifactStore store, final String path )
    {
        logger.trace( "Updating merged metadata file: {} in store: {}", path, store.getKey() );

        final Transfer item = fileManager.getStorageReference( store, path );
        final boolean isMetadata = item.getPath().endsWith( MavenMetadataMerger.METADATA_NAME );

        logger.trace( "Attempting to delete: {}", item );
        if ( item.exists() )
        {
            boolean result = deleteQuietly( store, item );
            logger.trace( "Deleted: {} (success? {})", item, result );
            if ( result && isMetadata )
            {
                Transfer info = fileManager.getStorageReference( store, path + GroupMergeHelper.MERGEINFO_SUFFIX );
                deleteQuietly( store, info );
            }
            if ( fileEvent != null )
            {
                logger.trace( "Firing deletion event for: {}", item );
                fileEvent.fire( new FileDeletionEvent( item, new EventMetadata() ) );
            }
            return result;
        }
        else if ( isMetadata )
        {
            // we should return true here to trigger cache cleaning, because file not exists in store does not mean
            // metadata not exists in cache.
            logger.debug( "Metadata clean for {}: metadata not existed, skip deletion and mark as deleted", item );
            return true;
        }
        return false;
    }

    private boolean deleteQuietly( final ArtifactStore store, final Transfer item )
    {
        try
        {
            return fileManager.delete( store, item.getPath(),
                                       new EventMetadata().set( StoreDataManager.IGNORE_READONLY, true ) );
        }
        catch ( IndyWorkflowException e )
        {
            logger.warn( "Deletion failed for metadata clear, transfer: {}, reason: {}", item, e.getMessage() );
        }
        return false;
    }

}