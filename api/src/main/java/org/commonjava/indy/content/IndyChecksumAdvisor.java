package org.commonjava.indy.content;

import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.checksum.ChecksummingDecoratorAdvisor;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;

import java.util.Optional;

/**
 * Created by jdcasey on 5/5/17.
 */
public interface IndyChecksumAdvisor
{
    Optional<ChecksummingDecoratorAdvisor.ChecksumAdvice> getChecksumReadAdvice( Transfer transfer,
                                                                             TransferOperation operation,
                                                                             EventMetadata eventMetadata );

    Optional<ChecksummingDecoratorAdvisor.ChecksumAdvice> getChecksumWriteAdvice( Transfer transfer,
                                                                                 TransferOperation operation,
                                                                                 EventMetadata eventMetadata );
}
