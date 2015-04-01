package org.commonjava.aprox.ftest.core.fixture;

import java.io.IOException;
import java.io.InputStream;

public class ReluctantInputStream
    extends InputStream
{
    private final byte[] data;

    private int idx = 0;

    public ReluctantInputStream( final byte[] data )
    {
        this.data = data;
    }
    
    public boolean hasNext()
    {
        return idx < data.length;
    }

    public synchronized void next()
    {
        notifyAll();
    }

    @Override
    public synchronized int read()
        throws IOException
    {
        if ( idx >= data.length )
        {
            return -1;
        }

        // don't send anything until pinged.
        try
        {
            wait();
        }
        catch ( final InterruptedException e )
        {
            return -1;
        }

        return data[idx++];
    }

    @Override
    public void close()
        throws IOException
    {
        idx = data.length;
        super.close();
    }

}