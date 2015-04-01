package org.commonjava.aprox.ftest.core.fixture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.client.core.Aprox;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.model.core.StoreKey;

public class DelayedDownload
    implements Runnable
{
    private final long initialDelay;
    private final Aprox client;
    
    private final StoreKey key;
    private final String path;
    
    private long startTime;
    private long endTime;
    private ByteArrayOutputStream content;

    private final CountDownLatch latch;
    
    public DelayedDownload( final Aprox client, final StoreKey key, final String path, final long initialDelay,
                            final CountDownLatch latch )
    {
        this.client = client;
        this.key = key;
        this.path = path;
        this.initialDelay = initialDelay;
        this.latch = latch;
    }
    
    @Override
    public void run()
    {
        if ( initialDelay > 0 )
        {
            try
            {
                Thread.sleep( initialDelay );
            }
            catch ( final InterruptedException e )
            {
                return;
            }
        }
        
        startTime = System.nanoTime();
        InputStream in = null;
        content = new ByteArrayOutputStream();
        
        try
        {
            in = client.content().get( key.getType(), key.getName(), path );
            IOUtils.copy(in, content);
        }
        catch ( AproxClientException | IOException e )
        {
            e.printStackTrace();
        }
        finally
        {
            IOUtils.closeQuietly( in );
        }
        
        endTime = System.nanoTime();
        latch.countDown();
    }
    
    public long getStartTime()
    {
        return startTime;
    }
    
    public long getEndTime()
    {
        return endTime;
    }
    
    public ByteArrayOutputStream getContent()
    {
        return content;
    }
}