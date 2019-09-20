package org.commonjava.indy.filer.def;

import com.codahale.metrics.Timer;
import org.commonjava.indy.metrics.RequestContextHelper;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Function;

public class TimingOutputStream
        extends FilterOutputStream
{
    private static final String RAW_IO_WRITE = "io.raw.write";

    private Long nanos;

    private Function<String, Timer.Context> timerProvider;

    private Timer.Context timer;

    public TimingOutputStream( final OutputStream stream, Function<String, Timer.Context> timerProvider )
    {
        super( stream );
        this.timerProvider = timerProvider == null ? (s)->null : timerProvider;
    }

    @Override
    public void write( final int b )
            throws IOException
    {
        if ( nanos == null )
        {
            nanos = System.nanoTime();
            timer = timerProvider.apply( RAW_IO_WRITE );
        }

        super.write( b );
    }

    @Override
    public void write( final byte[] b )
            throws IOException
    {
        if ( nanos == null )
        {
            nanos = System.nanoTime();
            timer = timerProvider.apply( RAW_IO_WRITE );
        }

        super.write( b );
    }

    @Override
    public void write( final byte[] b, final int off, final int len )
            throws IOException
    {
        if ( nanos == null )
        {
            nanos = System.nanoTime();
            timer = timerProvider.apply( RAW_IO_WRITE );
        }

        super.write( b, off, len );
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
