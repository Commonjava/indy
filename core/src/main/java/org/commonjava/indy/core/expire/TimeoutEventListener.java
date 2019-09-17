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
package org.commonjava.indy.core.expire;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.change.event.ArtifactStorePostUpdateEvent;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.maven.galley.event.FileAccessEvent;
import org.commonjava.maven.galley.event.FileDeletionEvent;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.commonjava.indy.util.LocationUtils.getKey;

@ApplicationScoped
public class TimeoutEventListener
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ScheduleManager scheduleManager;

    @Inject
    private ContentManager contentManager;

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private IndyObjectMapper objectMapper;

    @Inject
    private SpecialPathManager specialPathManager;

    public void onExpirationEvent( @Observes final SchedulerEvent event )
    {
        if ( !( event instanceof SchedulerTriggerEvent ) || !event.getJobType()
                                                                  .equals( ScheduleManager.CONTENT_JOB_TYPE ) )
        {
            return;
        }

        ContentExpiration expiration;
        try
        {
            expiration = objectMapper.readValue( event.getPayload(), ContentExpiration.class );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to read ContentExpiration from event payload.", e );
            return;
        }

        final StoreKey key = expiration.getKey();
        final String path = expiration.getPath();
        try
        {
            ArtifactStore store = storeManager.getArtifactStore( key );
            if ( store == null )
            {
                logger.warn( "Store: {} is no longer available. Nothing to do for path: {}", key, path );
            }

            boolean deleted = store == null || contentManager.delete( store, path );

            if ( !deleted )
            {
                logger.error("Failed to delete Transfer for: {} in: {} (for content timeout).", path, key );
            }
        }
        catch ( IndyWorkflowException e )
        {
            logger.error(
                    String.format( "Failed to retrieve Transfer for: %s in: %s (for content timeout). Reason: %s", path,
                                   key, e ), e );
        }
        catch ( IndyDataException e )
        {
            logger.error(
                    String.format( "Failed to retrieve ArtifactStore for: %s (for content timeout). Reason: %s", key, e ), e );
        }
    }

    public void onFileStorageEvent( @Observes final FileStorageEvent event )
    {
        final StoreKey key = getKey( event );
        if ( key == null )
        {
            return;
        }

        final Transfer transfer = event.getTransfer();
        final TransferOperation type = event.getType();

        switch ( type )
        {
            case UPLOAD:
            {
                try
                {
                    scheduleManager.setSnapshotTimeouts( key, transfer.getPath() );
                }
                catch ( final IndySchedulerException e )
                {
                    logger.error( "Failed to clean up metadata / set snapshot timeouts related to: " + transfer, e );
                }

                break;
            }
            case DOWNLOAD:
            {
                try
                {
                    scheduleManager.setProxyTimeouts( key, transfer.getPath() );
                }
                catch ( final IndySchedulerException e )
                {
                    logger.error( "Failed to clean up metadata / set proxy-cache timeouts related to: " + transfer, e );
                }

                break;
            }
            default:
            {
                break;
            }
        }
    }

    public void onFileAccessEvent( @Observes final FileAccessEvent event )
    {
        // TODO: handle this stuff in Weft somehow...
        Map original = MDC.getCopyOfContextMap();
        try
        {
            MDC.setContextMap( event.getMDCMap() );

            final StoreKey key = getKey( event );
            if ( key != null )
            {
                final Transfer transfer = event.getTransfer();
                final StoreType type = key.getType();

                if ( type == StoreType.hosted )
                {
                    try
                    {
                        scheduleManager.setSnapshotTimeouts( key, transfer.getPath() );
                    }
                    catch ( final IndySchedulerException e )
                    {
                        logger.error( "Failed to set snapshot timeouts related to: " + transfer, e );
                    }
                }
                else if ( type == StoreType.remote )
                {
                    SpecialPathInfo info = specialPathManager.getSpecialPathInfo( transfer );
                    if ( info == null || !info.isMetadata() )
                    {
                        logger.debug( "Accessed resource {} timeout will be reset.", transfer );
                        try
                        {
                            scheduleManager.setProxyTimeouts( key, transfer.getPath() );
                        }
                        catch ( final IndySchedulerException e )
                        {
                            logger.error( "Failed to set proxy-cache timeouts related to: " + transfer, e );
                        }
                    }
                    else
                    {
                        logger.debug( "Accessed resource {} is metadata. NOT rescheduling timeout!", transfer );
                    }
                }
            }
        }
        finally
        {
            if ( original != null && !original.isEmpty() )
            {
                MDC.setContextMap( original );
            }
            else
            {
                MDC.setContextMap( Collections.emptyMap() );
            }
        }
    }

    public void onFileDeletionEvent( @Observes final FileDeletionEvent event )
    {
    }

    public void onStoreUpdate( @Observes final ArtifactStorePostUpdateEvent event )
    {
        final ArtifactStoreUpdateType eventType = event.getType();
        if ( eventType == ArtifactStoreUpdateType.UPDATE )
        {
            for ( final ArtifactStore store : event )
            {
                final StoreKey key = store.getKey();
                final StoreType type = key.getType();
                if ( type == StoreType.hosted )
                {
                    logger.info( "[ADJUST TIMEOUTS] Adjusting snapshot expirations in: {}", store.getKey() );
                    try
                    {
                        scheduleManager.rescheduleSnapshotTimeouts( (HostedRepository) store );
                    }
                    catch ( final IndySchedulerException e )
                    {
                        logger.error( "Failed to update snapshot timeouts in: " + store.getKey(), e );
                    }
                }
                else if ( type == StoreType.remote )
                {
                    logger.info( "[ADJUST TIMEOUTS] Adjusting proxied-file expirations in: {}", store.getKey() );
                    try
                    {
                        scheduleManager.rescheduleProxyTimeouts( (RemoteRepository) store );
                    }
                    catch ( final IndySchedulerException e )
                    {
                        logger.error( "Failed to update proxy-cache timeouts in: " + store.getKey(), e );
                    }
                }
            }
        }
    }

}
