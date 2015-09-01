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
package org.commonjava.aprox.httprox.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.core.ctl.ContentController;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.util.HttpUtils;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.transport.htcli.model.HttpExchangeMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.channels.Channels;
import org.xnio.conduits.ConduitStreamSinkChannel;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jdcasey on 9/1/15.
 */
public class HttpConduitWrapper
        implements org.commonjava.aprox.subsys.http.HttpWrapper
{

    private final ConduitStreamSinkChannel sinkChannel;

    private List<String> headerLines;

    private final ContentController contentController;

    public HttpConduitWrapper( ConduitStreamSinkChannel channel, List<String> headerLines, ContentController contentController )
    {
        this.sinkChannel = channel;
        this.headerLines = headerLines;
        this.contentController = contentController;
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
            throws IOException, AproxWorkflowException
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
        try (FileChannel sourceChannel = new FileInputStream( txfr.getDetachedFile() ).getChannel())
        {
            writeStatus( ApplicationStatus.OK );
            writeHeader( ApplicationHeader.content_length, Long.toString( txfr.length() ) );
            writeHeader( ApplicationHeader.content_type, contentController.getContentType( path ) );
            writeHeader( ApplicationHeader.last_modified, HttpUtils.formatDateHeader( txfr.lastModified() ) );

            if ( writeBody )
            {
                sinkChannel.write( ByteBuffer.wrap( "\r\n".getBytes() ) );

                Channels.transferBlocking( sinkChannel, sourceChannel, 0, txfr.length() );
                txfr.touch( eventMetadata );
            }
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
        for ( final String line : headerLines )
        {
            final String upperLine = line.toUpperCase();
            if ( upperLine.startsWith( name.toUpperCase() ) )
            {
                final String[] parts = line.split( " " );
                if ( parts.length > 1 )
                {
                    result.add( parts[1] );
                }
            }
        }

        return result;
    }

    public void close()
            throws IOException
    {
        sinkChannel.flush();
        sinkChannel.shutdownWrites();
    }
}
