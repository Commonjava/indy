package org.commonjava.indy.event.audit;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentDigester;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
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
import org.commonjava.maven.galley.io.checksum.ContentDigest;
import org.commonjava.maven.galley.io.checksum.TransferMetadata;
import org.commonjava.maven.galley.model.Transfer;
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

    private final Logger logger = LoggerFactory.getLogger( "org.commonjava.indy.event.audit" );

    public void onFileAccess( @Observes final org.commonjava.maven.galley.event.FileAccessEvent event )
    {

        FileEvent fileEvent = new FileEvent( FileEventType.ACCESS );
        transformFileEvent( event, fileEvent );
        eventPublisher.publishFileEvent( fileEvent );

    }

    public void onFileUpload( @Observes final org.commonjava.maven.galley.event.FileStorageEvent event )
    {

        FileEvent fileEvent = new FileEvent( FileEventType.STORAGE );
        transformFileEvent( event, fileEvent );
        eventPublisher.publishFileEvent( fileEvent );
    }

    public void onPromoteComplete( @Observes final PromoteCompleteEvent event )
    {
        FileGroupingEvent fileGroupingEvent = new FileGroupingEvent( FileGroupingEventType.BY_PATH_PROMOTION );
        transformFileGroupingEvent( event, fileGroupingEvent );
        eventPublisher.publishFileGroupingEvent( fileGroupingEvent );
    }

    private void transformFileEvent( org.commonjava.maven.galley.event.FileEvent event, FileEvent fileEvent )
    {
        EventMetadata metadata = event.getEventMetadata();
        final TrackingKey trackingKey = (TrackingKey) metadata.get( "tracking-id" );
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

        fileEvent.setTargetPath( transfer.getPath() );
        fileEvent.setNodeId( "" );
        fileEvent.setSessionId( trackingKey.getId() );
        fileEvent.setTimestamp( new Date() );

        final KeyedLocation keyedLocation = (KeyedLocation) transfer.getLocation();
        final String path = transfer.getPath();
        try
        {
            StoreKey affectedStore = keyedLocation.getKey();
            final Transfer txfr = downloadManager.getStorageReference( keyedLocation.getKey(), transfer.getPath() );
            if ( txfr != null )
            {

                if ( StoreType.remote == affectedStore.getType() )
                {
                    final RemoteRepository repo = (RemoteRepository) storeManager.getArtifactStore( affectedStore );
                    if ( repo != null )
                    {
                        fileEvent.setTargetLocation( repo.getUrl() );
                    }
                }

                TransferMetadata artifactData = contentDigester.digest( affectedStore, path, event.getEventMetadata() );
                Map<String, String> extra = new HashMap<>();
                extra.put( ContentDigest.MD5.name(), artifactData.getDigests().get( ContentDigest.MD5 ) );
                extra.put( ContentDigest.SHA_1.name(), artifactData.getDigests().get( ContentDigest.SHA_1 ) );
                extra.put( ContentDigest.SHA_256.name(), artifactData.getDigests().get( ContentDigest.SHA_256 ) );
                extra.put( "Size", String.valueOf( artifactData.getSize() ) );

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

        Map<String, String> extra = new HashMap<>();
        extra.put( "source", source.toString() );
        extra.put( "target", target.toString() );

        fileGroupingEvent.setExtra( extra );
        fileGroupingEvent.setTimestamp( new Date() );

        TrackingKey trackingKey = getTrackingKey( source );
        if ( trackingKey == null )
        {
            logger.trace( "No tracking key found to: {}", source );
            return;
        }

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
