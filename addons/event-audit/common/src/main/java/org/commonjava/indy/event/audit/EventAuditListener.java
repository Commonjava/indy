package org.commonjava.indy.event.audit;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentDigester;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.event.audit.conf.EventAuditConfig;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.promote.change.PromoteChangeManager;
import org.commonjava.indy.promote.change.event.PathsPromoteCompleteEvent;
import org.commonjava.indy.promote.change.event.PromoteCompleteEvent;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.event.FileAccessEvent;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.io.checksum.ContentDigest;
import org.commonjava.maven.galley.io.checksum.TransferMetadata;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.propulsor.content.audit.model.FileEvent;
import org.commonjava.propulsor.content.audit.model.FileEventType;
import org.commonjava.propulsor.content.audit.model.FileGroupingEvent;
import org.commonjava.propulsor.content.audit.model.FileGroupingEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.commonjava.indy.model.core.StoreType.group;

@ApplicationScoped
public class EventAuditListener
{

    @Inject
    DownloadManager downloadManager;

    @Inject
    ContentDigester contentDigester;

    @Inject
    StoreDataManager storeManager;

    @Inject
    ISPNEventPublisher eventPublisher;

    @Inject
    PromoteChangeManager promoteChangeManager;

    @Inject
    EventAuditConfig eventAuditConfig;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public void onFileAccess( @Observes final FileAccessEvent event )
    {

        if ( !eventAuditConfig.isEnabled() )
        {
            return;
        }
        FileEvent fileEvent = new FileEvent( FileEventType.ACCESS );
        transformFileEvent( event, fileEvent );
        eventPublisher.publishFileEvent( fileEvent );

    }

    public void onFileUpload( @Observes final FileStorageEvent event )
    {

        if ( !eventAuditConfig.isEnabled() )
        {
            return;
        }

        if ( TransferOperation.UPLOAD != event.getType() )
        {
            logger.trace( "Not a file upload from client; skipping audit." );
            return;
        }

        FileEvent fileEvent = new FileEvent( FileEventType.STORAGE );
        transformFileEvent( event, fileEvent );
        eventPublisher.publishFileEvent( fileEvent );
    }

    public void onPromoteComplete( @Observes final PromoteCompleteEvent event )
    {
        if ( !eventAuditConfig.isEnabled() )
        {
            return;
        }

        FileGroupingEvent fileGroupingEvent = new FileGroupingEvent( FileGroupingEventType.BY_PATH_PROMOTION );
        transformFileGroupingEvent( event, fileGroupingEvent );
        eventPublisher.publishFileGroupingEvent( fileGroupingEvent );
    }

    private void transformFileEvent( org.commonjava.maven.galley.event.FileEvent event, FileEvent fileEvent )
    {
        EventMetadata metadata = event.getEventMetadata();
        final TrackingKey trackingKey = (TrackingKey) metadata.get( EventConstants.TRACKING_KEY );
        if ( trackingKey == null )
        {
            logger.trace( "No tracking key. Skip audit." );
            return;
        }

        Transfer transfer = event.getTransfer();
        if ( transfer == null )
        {
            logger.trace( "No transfer. Skip audit." );
            return;
        }

        final Location location = transfer.getLocation();
        if ( !( location instanceof KeyedLocation ) )
        {
            logger.trace( "Not in a keyed location: {}", transfer );
            return;
        }

        try
        {
            final KeyedLocation keyedLocation = (KeyedLocation) location;
            final StoreKey affectedStore = keyedLocation.getKey();
            if ( affectedStore.getType() == group )
            {
                logger.trace( "Not auditing content stored directly in group: {}. This content is generally aggregated metadata, and can be recalculated. Groups may not be stable in some build environments",
                              affectedStore );
                return;
            }

            final String path = transfer.getPath();

            fileEvent.setTargetPath( path );
            //TODO figure out what's the NodeId
            fileEvent.setNodeId( "" );
            fileEvent.setSessionId( trackingKey.getId() );
            //fileEvent.setTimestamp( new Date() );

            final Transfer txfr = downloadManager.getStorageReference( affectedStore, path );
            if ( txfr != null && txfr.exists() )
            {

                TransferMetadata artifactData = contentDigester.digest( affectedStore, path, metadata );
                Map<String, String> extra = new HashMap<>();
                extra.put( ContentDigest.MD5.name(), artifactData.getDigests().get( ContentDigest.MD5 ) );
                extra.put( ContentDigest.SHA_1.name(), artifactData.getDigests().get( ContentDigest.SHA_1 ) );
                extra.put( ContentDigest.SHA_256.name(), artifactData.getDigests().get( ContentDigest.SHA_256 ) );
                extra.put( EventConstants.SIZE, String.valueOf( artifactData.getSize() ) );
                extra.put( EventConstants.PATH, path );
                extra.put( EventConstants.STORE_KEY, affectedStore.toString() );
                extra.put( EventConstants.ACCESS_CHANNEL, affectedStore.getPackageType() );
                if ( event instanceof FileStorageEvent )
                {
                    extra.put( EventConstants.STORE_EFFECT, ( (FileStorageEvent) event ).getType().name() );
                }

                if ( StoreType.remote == affectedStore.getType())
                {
                    final RemoteRepository repo = (RemoteRepository) storeManager.getArtifactStore( affectedStore );
                    if ( repo != null )
                    {
                        extra.put( EventConstants.SOURCE_LOCATION, repo.getUrl() );
                        extra.put( EventConstants.SOURCE_PATH, txfr.getPath() );
                    }
                }

                //TODO fix this, it should be the url of indy, or recalculate it in AuditQuery side.
                fileEvent.setTargetLocation( "" );
                fileEvent.setChecksum( artifactData.getDigests().get( ContentDigest.MD5 ) );

                fileEvent.setExtra( extra );
            }

        }
        catch ( final IndyWorkflowException | IndyDataException e )
        {
            logger.error( String.format( "Failed to transform file event. Reason: %s", e.getMessage() ), e );
        }
    }

    private void transformFileGroupingEvent( PromoteCompleteEvent event, FileGroupingEvent fileGroupingEvent )
    {

        PathsPromoteCompleteEvent pathsPromoteCompleteEvent = null;
        if ( event instanceof PathsPromoteCompleteEvent )
        {
            pathsPromoteCompleteEvent = (PathsPromoteCompleteEvent) event;
        }
        else
        {
            logger.trace( "Unsupported grouping event: {}", event.getClass() );
            return;
        }

        PathsPromoteResult promoteResult = pathsPromoteCompleteEvent.getPromoteResult();

        String error = promoteResult.getError();
        if ( error != null )
        {
            logger.trace( "Error in promoteResult, skip audit." );
            return;
        }

        Set<String> paths = promoteResult.getCompletedPaths();
        if ( paths.isEmpty() )
        {
            logger.trace( "No completedPaths, skip audit." );
            return;
        }

        PathsPromoteRequest req = promoteResult.getRequest();
        StoreKey source = req.getSource();
        StoreKey target = req.getTarget();

        TrackingKey trackingKey = getTrackingKey( source );
        if ( trackingKey == null )
        {
            logger.trace( "No tracking key found to: {}", source );
            return;
        }

        Map<String, String> extra = new HashMap<>();
        extra.put( EventConstants.SOURCE, source.toString() );
        extra.put( EventConstants.TARGET, target.toString() );

        fileGroupingEvent.setExtra( extra );
        fileGroupingEvent.setTimestamp( new Date() );
        fileGroupingEvent.setSessionId( trackingKey.getId() );
    }

    private TrackingKey getTrackingKey( StoreKey source )
    {
        if ( source.getType() == StoreType.hosted )
        {
            return new TrackingKey( promoteChangeManager.getTrackingIdFormatter().format( source ) );
        }
        else
        {
            /* TODO: For remote, we can not get the tracking id by solely source repo name.
             * E.g., we promote ant from { "storeKey" : "maven:remote:central", "path" : "/ant/ant-launcher/1.6.5/ant-launcher-1.6.5.jar" }
             * into some hosted repo shared-imports, we really need to adjust all of those tracking records.
             *
             * One workaround is not to promote any remote repo artifact to hosted, or promote it with purse as false so the original
             * paths were still valid for a reproducer build.
             */
            return null;
        }
    }

}
