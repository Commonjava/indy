package org.commonjava.indy.event.publisher;

import org.commonjava.event.common.EventMetadata;
import org.commonjava.event.file.FileEvent;
import org.commonjava.event.file.FileEventType;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.content.ContentDigester;
import org.commonjava.indy.core.conf.IndyEventHandlerConfig;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.subsys.kafka.IndyKafkaProducer;
import org.commonjava.indy.subsys.kafka.conf.KafkaConfig;
import org.commonjava.maven.galley.event.FileAccessEvent;
import org.commonjava.maven.galley.event.FileDeletionEvent;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.io.checksum.ContentDigest;
import org.commonjava.maven.galley.io.checksum.TransferMetadata;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Date;
import java.util.Map;

@ApplicationScoped
public class KafkaEventPublisher
        implements FileEventPublisher
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyEventHandlerConfig handlerConfig;

    @Inject
    private IndyConfiguration indyConfig;

    @Inject
    StoreDataManager storeManager;

    @Inject
    ContentDigester contentDigester;

    @Inject
    IndyKafkaProducer kafkaProducer;

    @Inject
    KafkaConfig kafkaConfig;

    @Inject
    IndyObjectMapper objectMapper;

    @SuppressWarnings( "unused" )
    public void onFileDelete( @Observes final FileDeletionEvent event )
    {
        handleEvent( event, new FileEvent( FileEventType.DELETE ) );
    }

    @SuppressWarnings( "unused" )
    public void onFileUpload( @Observes final FileStorageEvent event )
    {
        handleEvent( event, new FileEvent( FileEventType.STORAGE ) );
    }

    private void handleEvent( final org.commonjava.maven.galley.event.FileEvent galleyFileEvent, final FileEvent fileEvent )
    {
        if ( !IndyEventHandlerConfig.HANDLER_KAFKA.equals( handlerConfig.getFileEventHandler() ) )
        {
            return;
        }
        transformFileEvent( galleyFileEvent, fileEvent );
        publishFileEvent( fileEvent );
    }

    public void onFileAccess( @Observes final FileAccessEvent event )
    {
        handleEvent( event, new FileEvent( FileEventType.ACCESS ) );
    }

    private void transformFileEvent( org.commonjava.maven.galley.event.FileEvent galleyEvent, FileEvent fileEvent )
    {
        Transfer transfer = galleyEvent.getTransfer();
        if ( transfer == null )
        {
            logger.trace( "No transfer." );
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

            final String path = transfer.getPath();

            fileEvent.setTargetPath( path );
            fileEvent.setNodeId( indyConfig.getNodeId() );

            org.commonjava.maven.galley.event.EventMetadata galleyMetadata = galleyEvent.getEventMetadata();
            if ( galleyMetadata != null )
            {
                EventMetadata fileMetadata = fileEvent.getEventMetadata();
                if ( fileMetadata == null )
                {
                    fileMetadata = new EventMetadata();
                }
                for ( Map.Entry<Object, Object> galleyMetaItem : galleyMetadata )
                {
                    fileMetadata.set( galleyMetaItem.getKey(), galleyMetaItem.getValue() );
                }
                final TrackingKey trackingKey = (TrackingKey) galleyMetadata.get( "tracking-id" );
                if ( trackingKey != null )
                {
                    fileEvent.setSessionId( trackingKey.getId() );
                }
            }

            fileEvent.setTimestamp( new Date() );

            TransferMetadata artifactData = contentDigester.digest( affectedStore, path, galleyMetadata );
            fileEvent.setMd5( artifactData.getDigests().get( ContentDigest.MD5 ) );
            fileEvent.setSha1( artifactData.getDigests().get( ContentDigest.SHA_1 ) );
            fileEvent.setChecksum( artifactData.getDigests().get( ContentDigest.SHA_256 ) );
            fileEvent.setSize( artifactData.getSize() );
            fileEvent.setStoreKey( affectedStore.toString() );

            if ( StoreType.remote == affectedStore.getType() )
            {
                final RemoteRepository repo = (RemoteRepository) storeManager.getArtifactStore( affectedStore );
                if ( repo != null )
                {
                    fileEvent.setSourceLocation( repo.getUrl() );
                    fileEvent.setSourcePath( transfer.getPath() );
                }
            }

        }
        catch ( final IndyWorkflowException | IndyDataException e )
        {
            logger.error( String.format( "Failed to transform file event. Reason: %s", e.getMessage() ), e );
        }
    }

    @Override
    public void publishFileEvent( FileEvent fileEvent )
    {
        try
        {
            kafkaProducer.send( kafkaConfig.getFileEventTopic(), fileEvent, 60000 );
        }
        catch ( Throwable e )
        {
            logger.error( "Send file event to Kafka error, {}", e.getMessage(), e );
        }
    }
}
