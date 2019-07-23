/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.ftest.core.fixture;

import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "Starting: {}", Thread.currentThread().getName() );

        startTime = System.nanoTime();
        while( stream.hasNext() )
        {
            logger.debug( "TICK" );
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

        logger.info( "Stopping: {}", Thread.currentThread().getName() );

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