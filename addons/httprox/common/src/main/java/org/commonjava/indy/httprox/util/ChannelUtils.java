package org.commonjava.indy.httprox.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;

/**
 * Created by ruhan on 11/5/18.
 */
public class ChannelUtils
{
    private static Logger logger = LoggerFactory.getLogger( ChannelUtils.class );

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
                logger.debug( "Waiting for channel to flush..." );
                try
                {
                    Thread.sleep( 100 );
                }
                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
