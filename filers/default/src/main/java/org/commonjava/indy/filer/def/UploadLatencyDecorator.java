package org.commonjava.indy.filer.def;

import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.AbstractTransferDecorator;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;

import java.io.IOException;
import java.io.OutputStream;

public class UploadLatencyDecorator
        extends AbstractTransferDecorator
{
    @Override
    public OutputStream decorateWrite( final OutputStream stream, final Transfer transfer, final TransferOperation op,
                                       final EventMetadata metadata )
            throws IOException
    {
        if ( op == TransferOperation.UPLOAD )
        {
            return new TimingOutputStream( stream );
        }

        return super.decorateWrite( stream, transfer, op, metadata );
    }
}
