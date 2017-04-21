package org.commonjava.indy.content;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.checksum.ContentDigest;
import org.commonjava.maven.galley.io.checksum.TransferMetadata;
import org.commonjava.maven.galley.io.checksum.TransferMetadataConsumer;
import org.commonjava.maven.galley.model.Transfer;

/**
 * Created by jdcasey on 1/4/17.
 * Handles caching of content metadata (size, checksums), and also provides methods for calculating this metadata
 * on demand. Implements {@link TransferMetadataConsumer}, allowing it to passively cache calculated metadata as
 * files are written to storage.
 */
public interface ContentDigester
        extends TransferMetadataConsumer
{

    TransferMetadata getContentMetadata( Transfer transfer );

    TransferMetadata digest( final StoreKey affectedStore, final String s, final EventMetadata eventMetadata,
                             final ContentDigest... types )
            throws IndyWorkflowException;
}
