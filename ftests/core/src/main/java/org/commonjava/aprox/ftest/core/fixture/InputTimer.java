package org.commonjava.aprox.ftest.core.fixture;

import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.IOUtils;

public class InputTimer
    implements Runnable
{
    private final ReluctantInputStream stream;
    private final long byteDelay;
    
    private long startTime;
    private long endTime;

    private final CountDownLatch latch;
    
    public InputTimer( final ReluctantInputStream stream, final long byteDelay, final CountDownLatch latch )
    {
        this.stream = stream;
        this.byteDelay = byteDelay;
        this.latch = latch;
    }
    
    @Override
    public void run()
    {
        startTime = System.nanoTime();
        while( stream.hasNext() )
        {
            stream.next();
            
            try
            {
                Thread.sleep(byteDelay);
            }
            catch ( final InterruptedException e )
            {
                IOUtils.closeQuietly( stream );
                return;
            }
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
}