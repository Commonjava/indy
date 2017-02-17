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
package org.commonjava.indy.httprox.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.core.ctl.ContentController;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.model.util.HttpUtils;
import org.commonjava.indy.util.ApplicationHeader;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.transport.htcli.model.HttpExchangeMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.channels.Channels;
import org.xnio.conduits.ConduitStreamSinkChannel;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jdcasey on 9/1/15.
 */
public class HttpConduitWrapper
        implements org.commonjava.indy.subsys.http.HttpWrapper
{

    private final ConduitStreamSinkChannel sinkChannel;

    private HttpRequest httpRequest;

    private final ContentController contentController;

    private CacheProvider cacheProvider;

    public HttpConduitWrapper( ConduitStreamSinkChannel channel, HttpRequest httpRequest, ContentController contentController, CacheProvider cacheProvider )
    {
        this.sinkChannel = channel;
        this.httpRequest = httpRequest;
        this.contentController = contentController;
        this.cacheProvider = cacheProvider;
    }

    @Override
    public void writeError( final Throwable e )
            throws IOException
    {
        final String message =
                String.format( "%s:\n  %s", e.getMessage(), StringUtils.join( e.getStackTrace(), "\n  " ) );

        sinkChannel.write( ByteBuffer.wrap( message.getBytes() ) );
    }

    @Override
    public void writeHeader( final ApplicationHeader header, final String value )
            throws IOException
    {
        final ByteBuffer b = ByteBuffer.wrap( String.format( "%s: %s\r\n", header.key(), value ).getBytes() );
        sinkChannel.write( b );
    }

    @Override
    public void writeHeader( final String header, final String value )
            throws IOException
    {
        final ByteBuffer b = ByteBuffer.wrap( String.format( "%s: %s\r\n", header, value ).getBytes() );
        sinkChannel.write( b );
    }

    @Override
    public void writeStatus( final ApplicationStatus status )
            throws IOException
    {
        final ByteBuffer b =
                ByteBuffer.wrap( String.format( "HTTP/1.1 %d %s\r\n", status.code(), status.message() ).getBytes() );
        sinkChannel.write( b );
    }

    @Override
    public void writeStatus( final int code, final String message )
            throws IOException
    {
        final ByteBuffer b = ByteBuffer.wrap( String.format( "HTTP/1.1 %d %s\r\n", code, message ).getBytes() );
        sinkChannel.write( b );
    }

    public void writeNotFoundTransfer( RemoteRepository repo, String path )
            throws IOException, IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "No transfer found." );
        final HttpExchangeMetadata metadata = contentController.getHttpMetadata( repo.getKey(), path );
        if ( metadata == null )
        {
            logger.debug( "No transfer metadata." );
            writeStatus( ApplicationStatus.NOT_FOUND );
        }
        else
        {
            logger.debug( "Writing metadata from http exchange with upstream." );
            if ( metadata.getResponseStatusCode() == 500 )
            {
                logger.debug( "Translating 500 error upstream into 502" );
                writeStatus( 502, "Bad Gateway" );
            }
            else
            {
                logger.debug( "Passing through upstream status: " + metadata.getResponseStatusCode() );
                writeStatus( metadata.getResponseStatusCode(), metadata.getResponseStatusMessage() );
            }

            writeHeader( ApplicationHeader.content_type, contentController.getContentType( path ) );
            for ( final Map.Entry<String, List<String>> headerSet : metadata.getResponseHeaders().entrySet() )
            {
                final String key = headerSet.getKey();
                if ( ApplicationHeader.content_type.upperKey().equals( key ) )
                {
                    continue;
                }

                for ( final String value : headerSet.getValue() )
                {
                    writeHeader( headerSet.getKey(), value );
                }
            }
        }
    }

    public void writeExistingTransfer( Transfer txfr, boolean writeBody, String path, EventMetadata eventMetadata )
            throws IOException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Valid transfer found." );
        try(InputStream in = txfr.openInputStream( true, eventMetadata ))
        {
            final HttpExchangeMetadata metadata = contentController.getHttpMetadata( txfr );
            logger.trace( "Got HTTP metadata: {} for transfer: {}", metadata, txfr );

            writeStatus( ApplicationStatus.OK );

            Long headerContentLength = metadata != null ? metadata.getContentLength() : null;
            long bytes = metadata != null && headerContentLength != null ? metadata.getContentLength() : txfr.length();
            if ( bytes < 1 )
            {
                bytes = txfr.length();
            }

            if ( bytes > 0 )
            {
                writeHeader( ApplicationHeader.content_length, String.valueOf( bytes ) );
            }

            String lastMod =
                    metadata != null ? metadata.getLastModified() : null;

            if ( lastMod == null )
            {
                lastMod = HttpUtils.formatDateHeader( txfr.lastModified() );
            }

            if ( lastMod != null )
            {
                writeHeader( ApplicationHeader.last_modified, lastMod );
            }

            writeHeader( ApplicationHeader.content_type, contentController.getContentType( path ) );

            if ( writeBody )
            {
                sinkChannel.write( ByteBuffer.wrap( "\r\n".getBytes() ) );

                int capacity = 16384;
                ByteBuffer bbuf = ByteBuffer.allocate( capacity );
                byte[] buf = new byte[capacity];
                int read = -1;
                while ( ( read = in.read( buf ) ) > -1 )
                {
                    bbuf.clear();
                    bbuf.put( buf, 0, read );
                    bbuf.flip();
                    int written = 0;
                    do
                    {
                        written += sinkChannel.write( bbuf );
                    }
                    while ( written < read );
                }
            }
        }
        catch ( IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to retrieve http-metadata.json file for: %s. Reason: %s", txfr,
                                         e.getMessage() ), e );
        }
        finally
        {
            cacheProvider.cleanupCurrentThread();
        }
    }

    @Override
    public boolean isOpen()
    {
        return sinkChannel != null && sinkChannel.isOpen();
    }

    @Override
    public List<String> getHeaders( String name )
    {
        List<String> result = new ArrayList<>();
        Header[] headers = httpRequest.getHeaders( name );
        for ( final Header header : headers )
        {
            result.add( header.getValue() );
        }

        return result;
    }

    public void close()
            throws IOException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        boolean flushed = false;
        while ( !flushed )
        {
            flushed = sinkChannel.flush();
            if ( !flushed )
            {
                try
                {
                    logger.debug( "Waiting for sink channel to flush..." );
                    wait( 100 );
                }
                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }
            }
        }

        sinkChannel.shutdownWrites();
    }
}
