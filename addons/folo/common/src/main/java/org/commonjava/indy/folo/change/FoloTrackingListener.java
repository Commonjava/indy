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
package org.commonjava.indy.folo.change;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentDigester;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.folo.conf.FoloConfig;
import org.commonjava.indy.folo.ctl.FoloConstants;
import org.commonjava.indy.folo.data.FoloContentException;
import org.commonjava.indy.folo.data.FoloRecordCache;
import org.commonjava.indy.folo.model.StoreEffect;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.event.FileAccessEvent;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.io.checksum.ContentDigest;
import org.commonjava.maven.galley.io.checksum.TransferMetadata;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.net.MalformedURLException;
import java.util.Map;

import static org.commonjava.indy.model.core.StoreType.group;

@ApplicationScoped
public class FoloTrackingListener
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private FoloConfig foloConfig;

    @Inject
    private FoloRecordCache recordManager;

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private DownloadManager downloadManager;

    @Inject
    private ContentDigester contentDigester;

    public void onFileAccess( @Observes final FileAccessEvent event )
    {
        logger.trace( "FILE ACCESS: {}", event );

        EventMetadata metadata = event.getEventMetadata();
        final TrackingKey trackingKey = (TrackingKey) metadata.get( FoloConstants.TRACKING_KEY );
        if ( trackingKey == null )
        {
            logger.trace( "No tracking key for access to: {}", event.getTransfer() );
            return;
        }
        final AccessChannel accessChannel = (AccessChannel) metadata.get( FoloConstants.ACCESS_CHANNEL );

        final Transfer transfer = event.getTransfer();
        if ( transfer == null )
        {
            logger.trace( "No transfer: {}", event );
            return;
        }

        final Location location = transfer.getLocation();
        if ( !( location instanceof KeyedLocation ) )
        {
            logger.trace( "Not in a keyed location: {}", event.getTransfer() );
            return;
        }

        try
        {
            final KeyedLocation keyedLocation = (KeyedLocation) location;
            if ( !foloConfig.isGroupContentTracked() && keyedLocation.getKey().getType() == group )
            {
                logger.trace(
                        "NOT tracking content stored directly in group: {}. This content is generally aggregated metadata, and can be recalculated. Groups may not be stable in some build environments",
                        keyedLocation.getKey() );
                return;
            }

            logger.trace( "Tracking report: {} += {} in {} (DOWNLOAD)", trackingKey, transfer.getPath(),
                          keyedLocation.getKey() );

            //Here we need to think about npm metadata retrieving case. As almost all npm metadata retrieving is through
            // /$pkg from remote, but we use STORAGE_PATH in EventMetadata with /$pkg/package.json to store this metadata,
            //so the real path for this transfer should be /$pkg but its current path is /$pkg/package.json. We need to
            //think about if need to do the replacement here, especially for the originalUrl.
            recordManager.recordArtifact(
                    createEntry( trackingKey, keyedLocation.getKey(), accessChannel, transfer.getPath(),
                                 StoreEffect.DOWNLOAD, event.getEventMetadata() ) );
        }
        catch ( final FoloContentException | IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to record download: %s. Reason: %s", transfer, e.getMessage() ), e );
        }
    }

    public void onFileUpload( @Observes final FileStorageEvent event )
    {
        logger.trace( "FILE STORAGE: {}", event );

        if ( TransferOperation.UPLOAD != event.getType() )
        {
            logger.trace( "Not a file upload from client; skipping tracking of storage" );
            return;
        }

        EventMetadata metadata = event.getEventMetadata();
        final TrackingKey trackingKey = (TrackingKey) metadata.get( FoloConstants.TRACKING_KEY );
        if ( trackingKey == null )
        {
            logger.trace( "No tracking key. Not recording." );
            return;
        }
        final AccessChannel accessChannel = (AccessChannel) metadata.get( FoloConstants.ACCESS_CHANNEL );

        final Transfer transfer = event.getTransfer();
        if ( transfer == null )
        {
            logger.trace( "No transfer. Not recording." );
            return;
        }

        final Location location = transfer.getLocation();
        if ( !( location instanceof KeyedLocation ) )
        {
            logger.trace( "Invalid transfer source location: {}. Not recording.", location );
            return;
        }
        else if ( !foloConfig.isGroupContentTracked() && ( (KeyedLocation) location ).getKey().getType() == group )
        {
            logger.trace(
                    "NOT tracking content stored directly in group: {}. This content is generally aggregated metadata, and can be recalculated. Groups may not be stable in some build environments",
                    ( (KeyedLocation) location ).getKey() );
            return;
        }


        final TransferOperation op = event.getType();
        StoreEffect effect = null;
        switch ( op )
        {
            case DOWNLOAD:
            {
                effect = StoreEffect.DOWNLOAD;
                break;
            }
            case UPLOAD:
            {
                effect = StoreEffect.UPLOAD;
                break;
            }
            default:
            {
                logger.trace( "Ignoring transfer operation: {} for: {}", op, transfer );
                return;
            }
        }

        try
        {
            final KeyedLocation keyedLocation = (KeyedLocation) location;
            logger.trace( "Tracking report: {} += {} in {} ({})", trackingKey, transfer.getPath(),
                          keyedLocation.getKey(), effect );

            recordManager.recordArtifact(
                    createEntry( trackingKey, keyedLocation.getKey(), accessChannel, transfer.getPath(), effect,
                                 event.getEventMetadata() ) );
        }
        catch ( final FoloContentException | IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to record download: %s. Reason: %s", transfer, e.getMessage() ), e );
        }
    }

    private TrackedContentEntry createEntry( final TrackingKey trackingKey, final StoreKey affectedStore,
                                             final AccessChannel accessChannel, final String path,
                                             final StoreEffect effect, final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        TrackedContentEntry entry = null;
        final Transfer txfr = downloadManager.getStorageReference( affectedStore, path );
        if ( txfr != null )
        {
            try
            {
                String remoteUrl = null;
                if ( StoreType.remote == affectedStore.getType() )
                {
                    final RemoteRepository repo = (RemoteRepository) storeManager.getArtifactStore( affectedStore );
                    if ( repo != null )
                    {
                        remoteUrl = UrlUtils.buildUrl( repo.getUrl(), path );
                    }
                }

                TransferMetadata artifactData =
                        contentDigester.digest( affectedStore, path, eventMetadata );

                Map<ContentDigest, String> digests = artifactData.getDigests();
                //TODO: As localUrl needs a apiBaseUrl which is from REST service context, to avoid deep propagate
                //      of it, this step will be done in REST layer. Will think better way in the future.
                entry = new TrackedContentEntry( trackingKey, affectedStore, accessChannel, remoteUrl, path, effect,
                                                 artifactData.getSize(), digests.get( ContentDigest.MD5 ),
                                                 digests.get( ContentDigest.SHA_1 ),
                                                 digests.get( ContentDigest.SHA_256 ) );
            }
            catch ( final IndyDataException e )
            {
                throw new IndyWorkflowException(
                        "Cannot retrieve RemoteRepository: %s to calculate remote URL for: %s. Reason: %s", e,
                        trackingKey, path, e.getMessage() );
            }
            catch ( final MalformedURLException e )
            {
                throw new IndyWorkflowException( "Cannot format URL. Reason: %s", e, e.getMessage() );
            }

        }
        return entry;
    }

}
