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
package org.commonjava.indy.bind.jaxrs.util;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.dto.CreationDTO;
import org.commonjava.indy.model.util.HttpUtils;
import org.commonjava.indy.util.ApplicationContent;
import org.commonjava.indy.util.ApplicationHeader;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.indy.util.UriFormatter;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.transport.htcli.model.HttpExchangeMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ResponseUtils
{

    private final static Logger LOGGER = LoggerFactory.getLogger( ResponseUtils.class );

    private ResponseUtils()
    {
    }

    public static Response formatRedirect( final URI uri )
            throws URISyntaxException
    {
        return Response.status( Status.MOVED_PERMANENTLY ).location( uri ).build();
    }

    public static Response formatCreatedResponse( final String baseUri, final UriFormatter uriFormatter,
                                                  final String... params )
            throws URISyntaxException
    {
        final URI location = new URI( uriFormatter.formatAbsolutePathTo( baseUri, params ) );
        return Response.created( location ).build();
    }

    public static Response formatCreatedResponseWithJsonEntity( final URI location, final Object dto,
                                                                final ObjectMapper objectMapper )
    {
        if ( dto == null )
        {
            return Response.noContent().build();
        }

        Response response;
        try
        {
            response = Response.created( location )
                               .entity( objectMapper.writeValueAsString( dto ) )
                               .type( ApplicationContent.application_json )
                               .build();
        }
        catch ( final JsonProcessingException e )
        {
            response = formatResponse( e, "Failed to serialize DTO to JSON: " + dto );
        }

        return response;
    }

    public static Response formatCreatedResponse( final String baseUri, final CreationDTO dto )
    {
        return Response.created( dto.getUri() ).entity( dto.getJsonResponse() ).build();
    }

    public static Response formatOkResponseWithJsonEntity( final String json )
    {
        return formatOkResponseWithEntity( json, ApplicationContent.application_json );
    }

    public static Response formatOkResponseWithJsonEntity( final Object dto, final ObjectMapper objectMapper )
    {
        if ( dto == null )
        {
            return Response.noContent().build();
        }

        Response response;
        try
        {
            response =
                    Response.ok( objectMapper.writeValueAsString( dto ), ApplicationContent.application_json ).build();
        }
        catch ( final JsonProcessingException e )
        {
            response = formatResponse( e, "Failed to serialize DTO to JSON: " + dto );
        }

        return response;
    }

    public static ResponseBuilder setInfoHeaders( final ResponseBuilder builder, final Transfer item, final StoreKey sk,
                                                  final String path, final boolean includeContentLength,
                                                  final String contentType,
                                                  final HttpExchangeMetadata exchangeMetadata )
            throws IndyWorkflowException
    {
        // I don't think we want to use the result from upstream; it's often junk...we should retain control of this.
        builder.header( ApplicationHeader.content_type.key(), contentType );

        boolean lastModSet = false;
        boolean lenSet = false;

        if ( exchangeMetadata != null )
        {
            for ( final Map.Entry<String, List<String>> headerSet : exchangeMetadata.getResponseHeaders().entrySet() )
            {
                final String key = headerSet.getKey();
                if ( ApplicationHeader.content_type.upperKey().equals( key ) )
                {
                    continue;
                }
                else if ( ApplicationHeader.last_modified.upperKey().equals( key ) )
                {
                    lastModSet = true;
                }
                else if ( ApplicationHeader.content_length.upperKey().equals( key ) )
                {
                    lenSet = true;
                    if ( !includeContentLength )
                    {
                        continue;
                    }
                }

                for ( final String value : headerSet.getValue() )
                {
                    builder.header( key, value );
                }
            }
        }

        if ( !lastModSet )
        {
            builder.header( ApplicationHeader.last_modified.key(), HttpUtils.formatDateHeader( item.lastModified() ) );
        }

        if ( includeContentLength && !lenSet )
        {
            builder.header( ApplicationHeader.content_length.key(), item.length() );
        }

        return builder;
    }

    public static ResponseBuilder setInfoHeaders( final ResponseBuilder builder, final File item,
                                                  final boolean includeContentLength, final String contentType )
            throws IndyWorkflowException
    {
        // I don't think we want to use the result from upstream; it's often junk...we should retain control of this.
        builder.header( ApplicationHeader.content_type.key(), contentType );

        builder.header( ApplicationHeader.last_modified.key(), HttpUtils.formatDateHeader( item.lastModified() ) );

        if ( includeContentLength )
        {
            builder.header( ApplicationHeader.content_length.key(), item.length() );
        }

        return builder;
    }

    public static Response formatResponseFromMetadata( final HttpExchangeMetadata metadata )
    {
        int code = metadata.getResponseStatusCode();
        // 500-level error; use 502 response.
        Logger logger = LoggerFactory.getLogger( ResponseUtils.class );
        logger.info( "Formatting response with code: {}", code );
        if ( code / 100 == 5 )
        {
            return Response.status( 502 ).build();
        }

        return Response.status( code ).build();
    }

    public static Response formatOkResponseWithEntity( final Object output, final String contentType )
    {
        return Response.ok( output ).type( contentType ).build();
    }

    public static Response formatBadRequestResponse( final String error )
    {
        final String msg = "{\"error\": \"" + error + "\"}\n";
        return Response.status( Status.BAD_REQUEST ).type( MediaType.APPLICATION_JSON ).entity( msg ).build();
    }

    public static Response formatResponse( final Throwable error )
    {
        return formulateResponse( null, error, null, false );
    }

    public static void throwError( final Throwable error )
    {
        formulateResponse( null, error, null, true );
    }

    public static Response formatResponse( final ApplicationStatus status, final Throwable error )
    {
        return formulateResponse( status, error, null, false );
    }

    public static void throwError( final ApplicationStatus status, final Throwable error )
    {
        formulateResponse( status, error, null, true );
    }

    public static Response formatResponse( final Throwable error, final String message )
    {
        return formulateResponse( null, error, message, false );
    }

    public static void throwError( final Throwable error, final String message )
    {
        formulateResponse( null, error, message, true );
    }

    public static Response formatResponse( final ApplicationStatus status, final Throwable error, final String message )
    {
        return formulateResponse( status, error, message, false );
    }

    public static void throwError( final ApplicationStatus status, final Throwable error, final String message )
    {
        formulateResponse( status, error, message, true );
    }

    private static Response formulateResponse( final ApplicationStatus status, final Throwable error,
                                               final String message, final boolean throwIt )
    {
        final String id = generateErrorId();
        final String msg = formatEntity( id, error, message ).toString();
        Status code = Status.INTERNAL_SERVER_ERROR;

        if ( status != null )
        {
            code = Status.fromStatusCode( status.code() );
            LOGGER.debug( "got error code from parameter: {}", code );
        }
        else if ( ( error instanceof IndyWorkflowException ) && ( (IndyWorkflowException) error ).getStatus() > 0 )
        {
            final int sc = ( (IndyWorkflowException) error ).getStatus();
            LOGGER.debug( "got error code from exception: {}", sc );
            code = Status.fromStatusCode( sc );
        }

        LOGGER.error( "Sending error response: {} {}\n{}", code.getStatusCode(), code.getReasonPhrase(), msg );

        Response response = Response.status( code )
                       .header( ApplicationHeader.content_type.key(), ApplicationContent.text_plain )
                       .entity( msg )
                       .build();

        if ( throwIt )
        {
            throw new WebApplicationException( error, response );
        }

        return response;
    }

    public static String generateErrorId()
    {
        return DigestUtils.sha256Hex( Thread.currentThread().getName() );

        //+ "@" + new SimpleDateFormat( "yyyy-MM-ddThhmmss.nnnZ" ).format( new Date() );
    }

    public static CharSequence formatEntity( final Throwable error )
    {
        return formatEntity( generateErrorId(), error, null );
    }

    public static CharSequence formatEntity( final String id, final Throwable error )
    {
        return formatEntity( id, error, null );
    }

    public static CharSequence formatEntity( final Throwable error, final String message )
    {
        return formatEntity( generateErrorId(), error, message );
    }

    public static CharSequence formatEntity( final String id, final Throwable error, final String message )
    {
        final StringWriter sw = new StringWriter();
        sw.append( "Id: " ).append( id );
        if ( message != null )
        {
            sw.append( "\nMessage: " ).append( message );
        }

        sw.append( error.getMessage() );

        final Throwable cause = error.getCause();
        if ( cause != null )
        {
            sw.append( "\nError:\n\n" );
            cause.printStackTrace( new PrintWriter( sw ) );
        }

        sw.write( '\n' );

        return sw.toString();
    }

    public static ResponseBuilder markDeprecated( final ResponseBuilder rb, final String alt )
    {
        return rb.header( ApplicationHeader.deprecated.key(), alt );
    }

}
