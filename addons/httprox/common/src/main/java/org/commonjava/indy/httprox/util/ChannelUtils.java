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
package org.commonjava.indy.httprox.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Created by ruhan on 11/5/18.
 */
public class ChannelUtils
{
    private static Logger logger = LoggerFactory.getLogger( ChannelUtils.class );

    public static final int DEFAULT_READ_BUF_SIZE = 1024 * 32;

    private static int MAX_FLUSH_RETRY_COUNT = 3;

    public static void flush( StreamSinkChannel channel ) throws IOException
    {
        int retry = 0;
        boolean flushed = false;
        while ( !flushed )
        {
            flushed = channel.flush();
            retry++;
            if ( retry >= MAX_FLUSH_RETRY_COUNT )
            {
                logger.debug( "Retry {} times and fail...", retry );
                break;
            }
            if ( !flushed )
            {
                wait( 100 );
            }
        }
    }

    public static void write( WritableByteChannel channel, ByteBuffer bbuf ) throws IOException
    {
        int written = 0;
        int size = bbuf.limit();
        do
        {
            written += channel.write( bbuf );
            if ( written < size )
            {
                wait( 100 );
            }
        }
        while ( written < size );
    }

    private static void wait( int milliseconds )
    {
        logger.debug( "Waiting for channel to flush..." );
        try
        {
            Thread.sleep( milliseconds );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
    }
}
