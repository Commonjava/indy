package org.commonjava.indy.filer.def;

import com.codahale.metrics.Timer;
import org.commonjava.indy.metrics.RequestContextHelper;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

public class TimingInputStream
        extends FilterInputStream
{
    private static final String RAW_IO_WRITE = "io.raw.read";

    private Long nanos;

    private Function<String, Timer.Context> timerProvider;

    private Timer.Context timer;

    public TimingInputStream( final InputStream stream, final Function<String, Timer.Context> timerProvider )
    {
        super( stream );
        this.timerProvider = timerProvider == null ? (s)->null : timerProvider;
    }

    @Override
    public int read()
            throws IOException
    {
        if ( nanos == null )
        {
            nanos = System.nanoTime();
            timer = timerProvider.apply( RAW_IO_WRITE );
        }

        return super.read();
    }

    @Override
    public int read( final byte[] b )
            throws IOException
    {
        if ( nanos == null )
        {
            nanos = System.nanoTime();
            timer = timerProvider.apply( RAW_IO_WRITE );
        }

        return super.read( b );
    }

    @Override
    public int read( final byte[] b, final int off, final int len )
            throws IOException
    {
        if ( nanos == null )
        {
            nanos = System.nanoTime();
            timer = timerProvider.apply( RAW_IO_WRITE );
        }

        return super.read( b, off, len );
    }

    @Override
    public void close()
            throws IOException
    {
        super.close();

        if ( nanos != null )
        {
            RequestContextHelper.setContext( RequestContextHelper.RAW_IO_WRITE_NANOS, System.nanoTime() - nanos );
        }

        if ( timer != null )
        {
            timer.stop();
        }
    }
}
