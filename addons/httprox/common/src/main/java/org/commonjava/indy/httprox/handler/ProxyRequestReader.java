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
package org.commonjava.indy.httprox.handler;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.config.MessageConstraints;
import org.apache.http.impl.DefaultHttpRequestFactory;
import org.apache.http.impl.io.DefaultHttpRequestParser;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.commonjava.indy.util.ApplicationHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.conduits.ConduitStreamSinkChannel;
import org.xnio.conduits.ConduitStreamSourceChannel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

public final class ProxyRequestReader
        implements ChannelListener<ConduitStreamSourceChannel>
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private ByteArrayOutputStream req = new ByteArrayOutputStream();

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

            logger.debug( "Request in progress is:\n\n'{}'", new String( req.toByteArray() ) );

            if ( read < 0 || headDone )
            {
                logger.debug( "request done. parsing." );
                MessageConstraints mc = MessageConstraints.DEFAULT;
                SessionInputBufferImpl inbuf = new SessionInputBufferImpl( new HttpTransportMetricsImpl(), 1024 );
                HttpRequestFactory requestFactory = new DefaultHttpRequestFactory();
                LineParser lp = new BasicLineParser();

                DefaultHttpRequestParser requestParser = new DefaultHttpRequestParser( inbuf, lp, requestFactory, mc );

                inbuf.bind( new ByteArrayInputStream( req.toByteArray() ) );

                try
                {
                    logger.debug( "Passing parsed http request off to response writer." );
                    HttpRequest request = requestParser.parse();
                    logger.debug( "Request contains {} header: '{}'", ApplicationHeader.authorization.key(),
                                  request.getHeaders( ApplicationHeader.authorization.key() ) );

                    writer.setHttpRequest( request );
                    sendResponse = true;
                }
                catch ( ConnectionClosedException e )
                {
                    logger.warn("Client closed connection. Aborting proxy request.");
                    sendResponse = false;
                    channel.resumeReads();
                }
                catch ( HttpException e )
                {
                    logger.error( "Failed to parse http request: " + e.getMessage(), e );
                    writer.setError( e );
                }
            }
            else
            {
                logger.debug( "request not finished. Pausing until more reads are available.'" );

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
        int read = -1;
        if ( headDone )
        {
            return read;
        }

        final ByteBuffer buf = ByteBuffer.allocate( 256 );
        logger.debug( "Starting read: {}", channel );

        PrintWriter printer = new PrintWriter( new OutputStreamWriter( req ) );

        readLoop:
        while ( ( read = channel.read( buf ) ) > 0 )
        {
            logger.debug( "Read {} bytes", read );

            buf.flip();
            final byte[] bbuf = new byte[buf.limit()];
            buf.get( bbuf );

            //            req.write( bbuf );
            // inefficient, but allows us to stop after header read...
            final String part = new String( bbuf );
//            logger.debug( part );
            for ( final char c : part.toCharArray() )
            {
                switch ( c )
                {
                    case '\r':
                    {
                        // check: \r\N\R\n
                        if ( lastChar == '\n' && req.size() > 0 )
                        {
                            // end of request head
                            logger.debug( "Detected end of request head. Breaking read loop." );
                            headDone = true;
                            break readLoop;
                        }
                    }
                    default:
                    {
                        req.write( (byte) c & 0x00FF );
                    }
                }

                lastChar = c;
            }
        }

        logger.debug( "Processed {} bytes", read );
        return read;
    }

}