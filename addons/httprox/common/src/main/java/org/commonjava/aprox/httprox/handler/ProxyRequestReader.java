package org.commonjava.aprox.httprox.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.conduits.ConduitStreamSinkChannel;
import org.xnio.conduits.ConduitStreamSourceChannel;

public final class ProxyRequestReader
    implements ChannelListener<ConduitStreamSourceChannel>
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private StringBuilder currentLine = new StringBuilder();

    private final List<String> headerLines = new ArrayList<>();

    private char lastChar = 0;

    private boolean headDone = false;

    private final ProxyResponseWriter writer;

    private final ConduitStreamSinkChannel sinkChannel;

    public ProxyRequestReader( final ProxyResponseWriter writer, final ConduitStreamSinkChannel sinkChannel )
    {
        this.writer = writer;
        this.sinkChannel = sinkChannel;
    }

    // TODO: May need to tune this to preserve request body.
    // TODO: NONE of the request headers (except authorization) are passed through!
    @Override
    public void handleEvent( final ConduitStreamSourceChannel channel )
    {
        boolean sendResponse = false;
        try
        {
            final int read = doRead( channel );

            if ( read < 0 || headDone )
            {
                writer.setHead( headerLines );
                sendResponse = true;
            }
            else
            {
                logger.debug( "request head not finished. Pausing until more reads are available." );
                channel.resumeReads();
            }
        }
        catch ( final IOException e )
        {
            writer.setError( e );
            sendResponse = true;
        }

        if ( sendResponse )
        {
            sinkChannel.resumeWrites();
            try
            {
                channel.shutdownReads();
            }
            catch ( final IOException e )
            {
                logger.debug( "failed to shutdown proxy request reads.", e );
            }
            //            IOUtils.closeQuietly( channel );
        }
    }

    private int doRead( final ConduitStreamSourceChannel channel )
        throws IOException
    {
        final ByteBuffer buf = ByteBuffer.allocate( 256 );
        logger.debug( "Starting read: {}", channel );
        int read = 0;
        readLoop: while ( ( read = channel.read( buf ) ) > 0 )
        {
            logger.debug( "Read {} bytes", read );

            buf.flip();
            final byte[] bbuf = new byte[buf.limit()];
            buf.get( bbuf );

            final String part = new String( bbuf );
            logger.debug( part );
            for ( final char c : part.toCharArray() )
            {
                if ( !headDone )
                {
                    if ( lastChar == '\n' && c == '\r' && !headerLines.isEmpty() )
                    {
                        // end of request head
                        logger.debug( "Detected end of request head. Breaking read loop." );
                        headDone = true;
                        break readLoop;
                    }
                    else if ( c == '\n' )
                    {
                        logger.debug( "Got line: '{}'", currentLine );
                        headerLines.add( currentLine.toString() );
                        currentLine.setLength( 0 );
                        currentLine = new StringBuilder();
                    }
                    else if ( c != '\r' )
                    {
                        currentLine.append( c );
                    }

                    lastChar = c;
                }
            }
        }

        return read;
    }

}