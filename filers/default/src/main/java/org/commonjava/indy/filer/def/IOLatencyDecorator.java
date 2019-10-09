package org.commonjava.indy.filer.def;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.CountingOutputStream;
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

    private Function<String, Meter> meterProvider;

    public IOLatencyDecorator( Function<String, Timer.Context> timerProvider, Function<String, Meter> meterProvider )
    {
        this.timerProvider = timerProvider;
        this.meterProvider = meterProvider;
    }

    @Override
    public InputStream decorateRead( final InputStream stream, final Transfer transfer, final EventMetadata metadata )
            throws IOException
    {
        return new TimingInputStream( new CountingInputStream( stream ), timerProvider, meterProvider );
    }

    @Override
    public OutputStream decorateWrite( final OutputStream stream, final Transfer transfer, final TransferOperation op,
                                       final EventMetadata metadata )
            throws IOException
    {
        if ( op == TransferOperation.UPLOAD )
        {
            return new TimingOutputStream( new CountingOutputStream( stream ), timerProvider, meterProvider );
        }

        return super.decorateWrite( stream, transfer, op, metadata );
    }
}
