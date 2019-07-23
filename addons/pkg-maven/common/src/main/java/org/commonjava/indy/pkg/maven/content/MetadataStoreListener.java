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
package org.commonjava.indy.pkg.maven.content;

import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.event.FileDeletionEvent;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import static org.commonjava.indy.IndyContentConstants.CHECK_CACHE_ONLY;
import static org.commonjava.indy.pkg.maven.content.group.MavenMetadataMerger.METADATA_NAME;

/**
 * Refer to MetadataCacheManager for how metadata.xml are cleaned when a group's membership is updated.
 */
@ApplicationScoped
public class MetadataStoreListener
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private MavenMetadataGenerator metadataGenerator;

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private MetadataCacheManager cacheManager;

    /**
     * Indy normally does not handle FileDeletionEvent when the cached metadata files were deleted due to store
     * enable/disable/delete, etc. Lately we add a force-deletion for group/remote-repo cache files. This requires to
     * delete affected group metadata files and clear ISPN cache too. We use this method for just this case, i.e.,
     * only if CHECK_CACHE_ONLY is true.
     */
    public void onMetadataFileForceDelete( @Observes final FileDeletionEvent event )
    {
        EventMetadata eventMetadata = event.getEventMetadata();
        if ( !Boolean.TRUE.equals( eventMetadata.get( CHECK_CACHE_ONLY ) ) )
        {
            return;
        }

        logger.trace( "Got file-delete event: {}", event );

        Transfer transfer = event.getTransfer();
        String path = transfer.getPath();

        if ( !path.endsWith( METADATA_NAME ) )
        {
            logger.trace( "Not {} , path: {}", METADATA_NAME, path );
            return;
        }

        Location loc = transfer.getLocation();

        if ( !( loc instanceof KeyedLocation ) )
        {
            logger.trace( "Ignore FileDeletionEvent, not a KeyedLocation, location: {}", loc );
            return;
        }

        KeyedLocation keyedLocation = (KeyedLocation) loc;
        StoreKey storeKey = keyedLocation.getKey();
        try
        {
            ArtifactStore store = storeManager.getArtifactStore( storeKey );
            metadataGenerator.clearAllMerged( store, path );
        }
        catch ( IndyDataException e )
        {
            logger.error( "Handle FileDeletionEvent failed", e );
        }
    }

}
