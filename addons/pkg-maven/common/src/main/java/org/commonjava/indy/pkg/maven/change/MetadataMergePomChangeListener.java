/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.core.change.event.IndyFileEventManager;
import org.commonjava.indy.core.content.group.GroupMergeHelper;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.pkg.maven.content.cache.MavenVersionMetadataCache;
import org.commonjava.indy.pkg.maven.content.group.MavenMetadataMerger;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.event.FileDeletionEvent;
import org.commonjava.maven.galley.event.FileEvent;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.util.LocationUtils.getKey;
import static org.commonjava.maven.galley.util.PathUtils.normalize;
import static org.commonjava.maven.galley.util.PathUtils.parentPath;

@javax.enterprise.context.ApplicationScoped
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
    @MavenVersionMetadataCache
    private CacheHandle<StoreKey, Map> versionMetadataCache;

    /**
     * this listener observes {@link org.commonjava.maven.galley.event.FileStorageEvent}
     * for a pom file, which means maven-metadata.xml will be cleared
     * when a version (pom) is uploaded.
     */
    public void onPomStorageEvent( @Observes final FileStorageEvent event )
    {
        metaClear( event );
    }

    /**
     * this listener observes {@link org.commonjava.maven.galley.event.FileDeletionEvent}
     * for a pom file, which means maven-metadata.xml will be cleared
     * when a version (pom) is removed.
     */
    public void onPomDeletionEvent( @Observes final FileDeletionEvent event )
    {
        metaClear( event );
    }

    private void metaClear( final FileEvent event )
    {
        final String path = event.getTransfer().getPath();

        if ( !path.endsWith( ".pom" ) )
        {
            return;
        }

        final StoreKey key = getKey( event );
        final String versionPath = normalize( parentPath( path ) );
        final String clearPath = normalize( normalize( parentPath( versionPath ) ), MavenMetadataMerger.METADATA_NAME );
        try
        {
            if ( hosted == key.getType() )
            {
                HostedRepository hosted = dataManager.query().getHostedRepository( key.getName() );
                try
                {
                    if ( doClear( hosted, clearPath ) )
                    {
                        versionMetadataCache.remove( hosted.getKey() );
                    }
                }
                catch ( final IOException e )
                {
                    logger.error(
                            String.format( "Failed to delete: %s from hosted: %s when pom version changed. Error: %s",
                                           path, hosted, e.getMessage() ), e );
                }

                final Set<Group> groups = dataManager.query().getGroupsAffectedBy( key );
                if ( groups != null )
                {
                    for ( final Group group : groups )
                    {
                        try
                        {
                            if ( doClear( group, clearPath ) )
                            {
                                versionMetadataCache.remove( group.getKey() );
                            }
                        }
                        catch ( final IOException e )
                        {
                            logger.error( String.format(
                                    "Failed to delete: %s from its group: %s when pom version changed. Error: %s", path,
                                    group, e.getMessage() ), e );
                        }
                    }
                }
            }
        }
        catch ( final IndyDataException e )
        {
            logger.warn( "Failed to regenerate maven-metadata.xml for artifacts after deployment to: {}"
                                 + "\nCannot retrieve associated groups: {}", e, key, e.getMessage() );
        }
    }

    private boolean doClear( final ArtifactStore store, final String path )
            throws IOException
    {
        boolean isCleared = false;
        logger.trace( "Updating merged metadata file: {} in store: {}", path, store.getKey() );

        final Transfer[] toDelete = { fileManager.getStorageReference( store, path ),
                fileManager.getStorageReference( store, path + GroupMergeHelper.MERGEINFO_SUFFIX ) };

        for ( final Transfer item : toDelete )
        {
            logger.trace( "Attempting to delete: {}", item );

            if ( item.exists() )
            {
                final boolean result = item.delete();
                logger.trace( "Deleted: {} (success? {})", item, result );

                if ( item.getPath().endsWith( MavenMetadataMerger.METADATA_NAME ) )
                {
                    isCleared = result;
                }
                if ( fileEvent != null )
                {
                    logger.trace( "Firing deletion event for: {}", item );
                    fileEvent.fire( new FileDeletionEvent( item, new EventMetadata() ) );
                }
            }
        }
        return isCleared;
    }
}