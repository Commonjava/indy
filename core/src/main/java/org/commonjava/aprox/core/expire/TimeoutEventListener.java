package org.commonjava.aprox.core.expire;

import static org.commonjava.aprox.util.LocationUtils.getKey;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Executor;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.change.event.ArtifactStoreDeleteEvent;
import org.commonjava.aprox.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.change.event.ArtifactStoreUpdateType;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;
import org.commonjava.maven.galley.event.FileAccessEvent;
import org.commonjava.maven.galley.event.FileDeletionEvent;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TimeoutEventListener
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ScheduleManager scheduleManager;

    @Inject
    private DownloadManager fileManager;

    @Inject
    private AproxObjectMapper objectMapper;

    @Inject
    @ExecutorConfig( daemon = true, priority = 7, named = "aprox-events" )
    private Executor executor;

    public void onExpirationEvent( @Observes final SchedulerEvent event )
    {
        if ( event.getEventType() != SchedulerEventType.TRIGGER || !event.getJobType()
                                                                         .equals( ScheduleManager.CONTENT_JOB_TYPE ) )
        {
            return;
        }

        executor.execute( new Runnable()
        {
            @Override
            public void run()
            {
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

                Transfer toDelete;
                try
                {
                    toDelete = fileManager.getStorageReference( key, path );
                }
                catch ( final AproxWorkflowException e )
                {
                    logger.error( String.format( "Failed to delete expired file for: %s, %s. Reason: %s", key, path,
                                                 e.getMessage() ), e );

                    return;
                }

                if ( toDelete.exists() )
                {
                    try
                    {
                        logger.info( "[EXPIRED; DELETE] {}", toDelete );
                        toDelete.delete();

                        scheduleManager.deleteJob( scheduleManager.groupName( key, ScheduleManager.CONTENT_JOB_TYPE ),
                                                   path );

                        scheduleManager.cleanMetadata( key, path );
                    }
                    catch ( final IOException e )
                    {
                        logger.error( String.format( "Failed to delete expired file: %s. Reason: %s",
                                                     toDelete.getFullPath(), e.getMessage() ), e );
                    }
                    catch ( final AproxSchedulerException e )
                    {
                        logger.error( String.format( "Failed to clean metadata related to expired file: %s. Reason: %s",
                                                     toDelete.getFullPath(), e.getMessage() ), e );
                    }

                }

                final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
                if ( pathInfo == null )
                {
                    return;
                }

                if ( pathInfo.isSnapshot() )
                {
                    scheduleManager.updateSnapshotVersions( key, path );
                }
            }
        } );
    }

    public void onFileStorageEvent( @Observes final FileStorageEvent event )
    {
        executor.execute( new Runnable()
        {
            @Override
            public void run()
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
                            scheduleManager.cleanMetadata( key, transfer.getPath() );
                            scheduleManager.setSnapshotTimeouts( key, transfer.getPath() );
                        }
                        catch ( final AproxSchedulerException e )
                        {
                            logger.error( "Failed to clean up metadata / set snapshot timeouts related to: " + transfer,
                                          e );
                        }

                        break;
                    }
                    case DOWNLOAD:
                    {
                        try
                        {
                            scheduleManager.cleanMetadata( key, transfer.getPath() );
                            scheduleManager.setProxyTimeouts( key, transfer.getPath() );
                        }
                        catch ( final AproxSchedulerException e )
                        {
                            logger.error( "Failed to clean up metadata / set proxy-cache timeouts related to: "
                                + transfer, e );
                        }

                        break;
                    }
                    default:
                    {
                        break;
                    }
                }
            }
        } );
    }

    public void onFileAccessEvent( @Observes final FileAccessEvent event )
    {
        executor.execute( new Runnable()
        {
            @Override
            public void run()
            {
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
                        catch ( final AproxSchedulerException e )
                        {
                            logger.error( "Failed to set snapshot timeouts related to: " + transfer, e );
                        }
                    }
                    else if ( type == StoreType.remote )
                    {
                        try
                        {
                            scheduleManager.setProxyTimeouts( key, transfer.getPath() );
                        }
                        catch ( final AproxSchedulerException e )
                        {
                            logger.error( "Failed to set proxy-cache timeouts related to: " + transfer, e );
                        }
                    }
                }
            }
        } );
    }

    public void onFileDeletionEvent( @Observes final FileDeletionEvent event )
    {
        executor.execute( new Runnable()
        {
            @Override
            public void run()
            {
                final StoreKey key = getKey( event );
                if ( key != null )
                {
                    try
                    {
                        scheduleManager.cancel( new StoreKeyMatcher( key, ScheduleManager.CONTENT_JOB_TYPE ),
                                                event.getTransfer()
                                                     .getPath() );
                    }
                    catch ( final AproxSchedulerException e )
                    {
                        logger.error( "Failed to cancel content-expiration timeout related to: " + event.getTransfer(),
                                      e );
                    }
                }
            }
        } );
    }

    public void onStoreUpdate( @Observes final ArtifactStoreUpdateEvent event )
    {
        executor.execute( new Runnable()
        {
            @Override
            public void run()
            {
                final ArtifactStoreUpdateType eventType = event.getType();
                if ( eventType == ArtifactStoreUpdateType.ADD_OR_UPDATE )
                {
                    for ( final ArtifactStore store : event )
                    {
                        final StoreKey key = store.getKey();
                        final StoreType type = key.getType();
                        if ( type == StoreType.hosted )
                        {
                            //                    logger.info( "[ADJUST TIMEOUTS] Adjusting snapshot expirations in: %s", store.getKey() );
                            try
                            {
                                scheduleManager.rescheduleSnapshotTimeouts( (HostedRepository) store );
                            }
                            catch ( final AproxSchedulerException e )
                            {
                                logger.error( "Failed to update snapshot timeouts in: " + store.getKey(), e );
                            }
                        }
                        else if ( type == StoreType.remote )
                        {
                            //                    logger.info( "[ADJUST TIMEOUTS] Adjusting proxied-file expirations in: %s", store.getKey() );
                            try
                            {
                                scheduleManager.rescheduleProxyTimeouts( (RemoteRepository) store );
                            }
                            catch ( final AproxSchedulerException e )
                            {
                                logger.error( "Failed to update proxy-cache timeouts in: " + store.getKey(), e );
                            }
                        }
                    }
                }
            }
        } );
    }

    public void onStoreDeletion( @Observes final ArtifactStoreDeleteEvent event )
    {
        executor.execute( new Runnable()
        {
            @Override
            public void run()
            {
                final StoreType type = event.getType();
                final Collection<String> names = event.getNames();

                for ( final String name : names )
                {
                    final StoreKey key = new StoreKey( type, name );
                    Transfer dir;
                    try
                    {
                        dir = fileManager.getStoreRootDirectory( key );
                    }
                    catch ( final AproxWorkflowException e )
                    {
                        logger.error( "Failed to cancel file expirations for deleted artifact store: {}. Error: {}", e,
                                      key, e.getMessage() );
                        return;
                    }

                    if ( dir.exists() && dir.isDirectory() )
                    {
                        try
                        {
                            logger.info( "[STORE REMOVED; DELETE] {}", dir.getFullPath() );
                            dir.delete();
                            scheduleManager.cancelAll( new StoreKeyMatcher( key, ScheduleManager.CONTENT_JOB_TYPE ) );
                        }
                        catch ( final IOException e )
                        {
                            logger.error( String.format( "Failed to delete storage for deleted artifact store: %s (dir: %s). Error: %s",
                                                         key, dir, e.getMessage() ), e );
                        }
                        catch ( final AproxSchedulerException e )
                        {
                            logger.error( String.format( "Failed to cancel file expirations for deleted artifact store: {} (dir: {}). Error: {}",
                                                         key, dir, e.getMessage() ), e );
                        }
                    }
                }
            }
        } );
    }

}
