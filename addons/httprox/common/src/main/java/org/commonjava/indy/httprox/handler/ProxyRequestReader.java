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
package org.commonjava.indy.httprox.handler;

import org.apache.commons.codec.binary.Hex;
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
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class ProxyRequestReader
        implements ChannelListener<ConduitStreamSourceChannel>
{
    private static final int AWAIT_READABLE_IN_MILLISECONDS = 100;

    private static final List<Character> HEAD_END = Collections.unmodifiableList(
            Arrays.asList( Character.valueOf( '\r' ), Character.valueOf( '\n' ), Character.valueOf( '\r' ),
                           Character.valueOf( '\n' ) ) );

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private ByteArrayOutputStream bReq;

    private PrintStream pReq;

    private boolean headDone = false;

    private final ProxyResponseWriter writer;

    private final ConduitStreamSinkChannel sinkChannel;

    private ProxySSLTunnel sslTunnel;

    private List<Character> lastFour = new ArrayList<>();

    public ProxyRequestReader( final ProxyResponseWriter writer, final ConduitStreamSinkChannel sinkChannel )
    {
        this.writer = writer;
        this.sinkChannel = sinkChannel;
    }

    // TODO: May need to tune this to preserve request body.
    // TODO: NONE of the request headers (except authorization) are passed through!
    @Override
    public void handleEvent( final ConduitStreamSourceChannel sourceChannel )
    {
        boolean sendResponse = false;
        try
        {
            final int read = doRead( sourceChannel );

            if ( read <= 0 )
            {
                logger.debug( "Reads: {} ", read );
                return;
            }

            byte[] bytes = bReq.toByteArray();

            if ( sslTunnel != null )
            {
                logger.debug( "Send to ssl tunnel, {}, bytes:\n\n {}\n", new String( bytes ), Hex.encodeHexString( bytes ) );
                directTo( sslTunnel );
                return;
            }

            logger.debug( "Request in progress is:\n\n{}", new String( bytes ) );

            if ( headDone )
            {
                logger.debug( "Request done. parsing." );
                MessageConstraints mc = MessageConstraints.DEFAULT;
                SessionInputBufferImpl inbuf = new SessionInputBufferImpl( new HttpTransportMetricsImpl(), 1024 );
                HttpRequestFactory requestFactory = new DefaultHttpRequestFactory();
                LineParser lp = new BasicLineParser();

                DefaultHttpRequestParser requestParser = new DefaultHttpRequestParser( inbuf, lp, requestFactory, mc );

                inbuf.bind( new ByteArrayInputStream( bytes ) );

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
                    sourceChannel.shutdownReads();
                }
                catch ( HttpException e )
                {
                    logger.error( "Failed to parse http request: " + e.getMessage(), e );
                    writer.setError( e );
                }
            }
            else
            {
                logger.debug( "Request not finished. Pausing until more reads are available." );
                sourceChannel.resumeReads();
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
        }
    }

    public void setProxySSLTunnel( ProxySSLTunnel sslTunnel )
    {
        this.sslTunnel = sslTunnel;
    }

    private void directTo( ProxySSLTunnel sslTunnel ) throws IOException
    {
        byte[] bytes = bReq.toByteArray();
        logger.trace( "Write client data to ssl tunnel, size: {}", bytes.length );
        sslTunnel.write( bytes );
    }

    private int doRead( final ConduitStreamSourceChannel channel )
            throws IOException
    {
        bReq = new ByteArrayOutputStream();
        pReq = new PrintStream( bReq );

        logger.debug( "Starting read: {}", channel );

        int total = 0;
        while ( true )
        {
            ByteBuffer buf = ByteBuffer.allocate( 1024 );
            channel.awaitReadable( AWAIT_READABLE_IN_MILLISECONDS, TimeUnit.MILLISECONDS );

            int read = channel.read( buf ); // return the number of bytes read, possibly zero, or -1

            logger.debug( "Read {} bytes", read );

            if ( read == -1 ) // return -1 if the channel has reached end-of-stream
            {
                if ( total == 0 ) // nothing read, return -1 to indicate the EOF
                {
                    return -1;
                }
                else
                {
                    return total;
                }
            }

            if ( read == 0 ) // no new bytes this time
            {
                return total;
            }

            total += read;

            buf.flip();
            byte[] bbuf = new byte[buf.limit()];
            buf.get( bbuf );

            if ( !headDone )
            {
                // allows us to stop after header read...
                final String part = new String( bbuf );
                for ( final char c : part.toCharArray() )
                {
                    switch ( c )
                    {
                        case '\n':
                        {
                            while ( lastFour.size() > 3 )
                            {
                                lastFour.remove(0);
                            }

                            lastFour.add( c );
                            try
                            {
                                if ( bReq.size() > 0 && HEAD_END.equals( lastFour ) )
                                {
                                    logger.debug( "Detected end of request headers." );
                                    headDone = true;

                                    logger.trace( "Proxy request header:\n{}\n", new String( bReq.toByteArray() ) );
                                }
                            }
                            finally
                            {
                                lastFour.remove( lastFour.size() - 1 );
                            }
                        }
                        default:
                        {
                            pReq.print( c );
                            lastFour.add( c );
                        }
                    }
                }
            }
            else
            {
                bReq.write( bbuf );
            }
        }
    }

}