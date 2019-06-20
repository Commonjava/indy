package org.commonjava.indy.core.content;

import org.commonjava.indy.content.IndyChecksumAdvisor;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.checksum.ChecksummingDecoratorAdvisor;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.commonjava.indy.core.content.ContentMetadataGenerator.FORCE_CHECKSUM_AND_WRITE;
import static org.commonjava.maven.galley.io.checksum.ChecksummingDecoratorAdvisor.ChecksumAdvice.CALCULATE_AND_WRITE;

public class RepairChecksumAdvisor
                implements IndyChecksumAdvisor
{
    @Override
    public Optional<ChecksummingDecoratorAdvisor.ChecksumAdvice> getChecksumReadAdvice( Transfer transfer,
                                                                                        TransferOperation operation,
                                                                                        EventMetadata eventMetadata )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        Location loc = transfer.getLocation();
        Object forceObj = eventMetadata.get( FORCE_CHECKSUM_AND_WRITE );
        boolean force = Boolean.TRUE.equals( forceObj ) || Boolean.parseBoolean( String.valueOf( forceObj ) );

        if ( force && ( loc instanceof KeyedLocation
                        && ( (KeyedLocation) loc ).getKey().getType() == StoreType.hosted ) )
        {
            logger.debug( "Enable checksumming for {} of: {} with advice: CALCULATE_AND_WRITE", operation, transfer );
            return Optional.of( CALCULATE_AND_WRITE );
        }

        logger.debug( "Skip checksumming for {} of: {}", operation, transfer );
        return Optional.empty();
    }

    @Override
    public Optional<ChecksummingDecoratorAdvisor.ChecksumAdvice> getChecksumWriteAdvice( Transfer transfer,
                                                                                         TransferOperation operation,
                                                                                         EventMetadata eventMetadata )
    {
        return Optional.empty();
    }
}
