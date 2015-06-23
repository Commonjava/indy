/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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

    private boolean missing;
    
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
            if ( in == null )
            {
                missing = true;
            }
            else
            {
                IOUtils.copy( in, content );
            }
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
    
    public boolean isMissing()
    {
        return missing;
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