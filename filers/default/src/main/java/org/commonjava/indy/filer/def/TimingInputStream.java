package org.commonjava.indy.filer.def;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.apache.commons.io.input.CountingInputStream;
import org.commonjava.indy.metrics.RequestContextHelper;
import org.commonjava.maven.galley.util.IdempotentCloseInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import static org.commonjava.indy.IndyContentConstants.NANOS_PER_SEC;

public class TimingInputStream
        extends IdempotentCloseInputStream
{
    private static final String RAW_IO_READ = "io.raw.read.timer";

    private static final String RAW_IO_READ_RATE = "io.raw.read.rate";

    private Long nanos;

    private Function<String, Timer.Context> timerProvider;

    private final Function<String, Meter> meterProvider;

    private Timer.Context timer;

    private Meter meter;

    public TimingInputStream( final CountingInputStream stream, final Function<String, Timer.Context> timerProvider,
                              final Function<String, Meter> meterProvider )
    {
        super( stream );
        this.timerProvider = timerProvider == null ? ( s ) -> null : timerProvider;
        this.meterProvider = meterProvider;
    }

    @Override
    public int read()
            throws IOException
    {
        initMetrics();
        return super.read();
    }

    @Override
    public int read( final byte[] b )
            throws IOException
    {
        initMetrics();
        return super.read( b );
    }

    @Override
    public int read( final byte[] b, final int off, final int len )
            throws IOException
    {
        initMetrics();
        return super.read( b, off, len );
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
                meter.mark( (long) ( ( (CountingInputStream) this.in ).getByteCount() / ( elapsed / NANOS_PER_SEC ) ) );
            }
        }
    }

    private void initMetrics()
    {
        if ( nanos == null )
        {
            nanos = System.nanoTime();
            timer = timerProvider.apply( RAW_IO_READ );
            meter = meterProvider.apply( RAW_IO_READ_RATE );
        }
    }
}
