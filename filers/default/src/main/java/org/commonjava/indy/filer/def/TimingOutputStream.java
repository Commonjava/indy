package org.commonjava.indy.filer.def;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.apache.commons.compress.utils.CountingInputStream;
import org.apache.commons.io.output.CountingOutputStream;
import org.commonjava.indy.metrics.RequestContextHelper;
import org.commonjava.maven.galley.util.IdempotentCloseOutputStream;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Function;

import static org.commonjava.indy.IndyContentConstants.NANOS_PER_SEC;

public class TimingOutputStream
        extends IdempotentCloseOutputStream
{
    private static final String RAW_IO_WRITE = "io.raw.write.timer";

    private static final String RAW_IO_WRITE_RATE = "io.raw.write.rate";

    private Long nanos;

    private Function<String, Timer.Context> timerProvider;

    private Function<String, Meter> meterProvider;

    private Timer.Context timer;

    private Meter meter;

    public TimingOutputStream( final CountingOutputStream stream, Function<String, Timer.Context> timerProvider, Function<String, Meter> meterProvider )
    {
        super( stream );
        this.timerProvider = timerProvider == null ? (s)->null : timerProvider;
        this.meterProvider = meterProvider;
    }

    @Override
    public void write( final int b )
            throws IOException
    {
        initMetrics();
        super.write( b );
    }

    @Override
    public void write( final byte[] b )
            throws IOException
    {
        initMetrics();
        super.write( b );
    }

    @Override
    public void write( final byte[] b, final int off, final int len )
            throws IOException
    {
        initMetrics();
        super.write( b, off, len );
    }

    @Override
    public void close()
            throws IOException
    {
        super.close();

        if ( nanos != null )
        {
            long elapsed = System.nanoTime() - nanos;

            RequestContextHelper.setContext( RequestContextHelper.RAW_IO_WRITE_NANOS, elapsed );

            if ( timer != null )
            {
                timer.stop();
            }

            if ( meter != null )
            {
                meter.mark( (long) ( ( (CountingOutputStream) this.out ).getByteCount() / ( elapsed / NANOS_PER_SEC ) ) );
            }
        }

    }

    private void initMetrics()
    {
        if ( nanos == null )
        {
            nanos = System.nanoTime();
            timer = timerProvider.apply( RAW_IO_WRITE );
            meter = meterProvider.apply( RAW_IO_WRITE_RATE );
        }
    }

}
