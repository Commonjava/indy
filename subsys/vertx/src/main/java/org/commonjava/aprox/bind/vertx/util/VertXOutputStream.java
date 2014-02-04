package org.commonjava.aprox.bind.vertx.util;

import java.io.IOException;
import java.io.OutputStream;

import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.WriteStream;

public class VertXOutputStream
    extends OutputStream
{

    private final WriteStream<?> response;

    private final byte[] buffer;

    private int counter = 0;

    public VertXOutputStream( final WriteStream<?> response )
    {
        this.response = response;

        // TODO is there a better size? Common MTU on TCP/IP is 1500, with about 1380 payload...is that better?
        buffer = new byte[16384];
    }

    @Override
    public synchronized void write( final int b )
        throws IOException
    {
        buffer[counter++] = (byte) b;
        if ( counter >= buffer.length )
        {
            flush();
        }
    }

    @Override
    public synchronized void flush()
        throws IOException
    {
        super.flush();
        if ( counter > 0 )
        {
            byte[] remaining = buffer;
            if ( counter < buffer.length )
            {
                remaining = new byte[counter];
                System.arraycopy( buffer, 0, remaining, 0, counter );
            }
            response.write( new Buffer( remaining ) );
            counter = 0;
        }
    }

    @Override
    public synchronized void close()
        throws IOException
    {
        flush();
        super.close();
    }

}
