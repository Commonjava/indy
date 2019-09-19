package org.commonjava.indy.filer.def;

import com.codahale.metrics.Timer;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.AbstractTransferDecorator;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;

public class IOLatencyDecorator
        extends AbstractTransferDecorator
{
    private Function<String, Timer.Context> timerProvider;

    public IOLatencyDecorator( Function<String, Timer.Context> timerProvider )
    {
        this.timerProvider = timerProvider;
    }

    @Override
    public InputStream decorateRead( final InputStream stream, final Transfer transfer, final EventMetadata metadata )
            throws IOException
    {
        return new TimingInputStream( stream, timerProvider );
    }

    @Override
    public OutputStream decorateWrite( final OutputStream stream, final Transfer transfer, final TransferOperation op,
                                       final EventMetadata metadata )
            throws IOException
    {
        if ( op == TransferOperation.UPLOAD )
        {
            return new TimingOutputStream( stream, timerProvider );
        }

        return super.decorateWrite( stream, transfer, op, metadata );
    }
}
