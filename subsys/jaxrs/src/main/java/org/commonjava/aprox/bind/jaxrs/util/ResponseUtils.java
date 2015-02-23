/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.bind.jaxrs.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.model.core.dto.CreationDTO;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.UriFormatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ResponseUtils
{

    private ResponseUtils()
    {
    }

    public static Response formatRedirect( final URI uri )
        throws URISyntaxException
    {
        return Response.status( Status.MOVED_PERMANENTLY )
                       .location( uri )
                       .build();
    }

    public static Response formatCreatedResponse( final String baseUri, final UriFormatter uriFormatter,
                                                  final String... params )
        throws URISyntaxException
    {
        final URI location = new URI( uriFormatter.formatAbsolutePathTo( baseUri, params ) );
        return Response.created( location )
                       .build();
    }

    public static Response formatCreatedResponseWithJsonEntity( final URI location, final Object dto,
                                                                final ObjectMapper objectMapper )
    {
        if ( dto == null )
        {
            return Response.noContent()
                           .build();
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
            response = formatResponse( e, "Failed to serialize DTO to JSON: " + dto, true );
        }

        return response;
    }

    public static Response formatCreatedResponse( final String baseUri, final CreationDTO dto )
    {
        return Response.created( dto.getUri() )
                       .entity( dto.getJsonResponse() )
                       .build();
    }

    public static Response formatOkResponseWithJsonEntity( final String json )
    {
        return formatOkResponseWithEntity( json, ApplicationContent.application_json );
    }

    public static Response formatOkResponseWithJsonEntity( final Object dto, final ObjectMapper objectMapper )
    {
        if ( dto == null )
        {
            return Response.noContent()
                           .build();
        }

        Response response;
        try
        {
            response = Response.ok( objectMapper.writeValueAsString( dto ), ApplicationContent.application_json )
                               .build();
        }
        catch ( final JsonProcessingException e )
        {
            response = formatResponse( e, "Failed to serialize DTO to JSON: " + dto, true );
        }

        return response;
    }

    public static Response formatOkResponseWithEntity( final Object output, final String contentType )
    {
        return Response.ok( output )
                       .type( contentType )
                       .build();
    }

    public static Response formatBadRequestResponse( final String error )
    {
        final String msg = "{\"error\": \"" + error + "\"}\n";
        return Response.status( Status.BAD_REQUEST )
                       .type( MediaType.APPLICATION_JSON )
                       .entity( msg )
                       .build();
    }

    public static Response formatResponse( final Throwable error )
    {
        return formatResponse( null, error, true );
    }

    public static Response formatResponse( final ApplicationStatus status, final Throwable error )
    {
        return formatResponse( status, error, true );
    }

    public static Response formatResponse( final Throwable error, final boolean includeExplanation )
    {
        return formatResponse( null, error, includeExplanation );
    }

    public static Response formatResponse( final Throwable error, final String message, final boolean includeExplanation )
    {
        return formatResponse( null, error, message, includeExplanation );
    }

    public static Response formatResponse( final ApplicationStatus status, final Throwable error,
                                           final boolean includeExplanation )
    {
        return formatResponse( status, error, null, includeExplanation );
    }

    public static Response formatResponse( final ApplicationStatus status, final Throwable error, final String message,
                                           final boolean includeExplanation )
    {
        ResponseBuilder rb;
        if ( status != null )
        {
            rb = Response.status( Status.fromStatusCode( status.code() ) );
        }
        else if ( ( error instanceof AproxWorkflowException ) && ( (AproxWorkflowException) error ).getStatus() > 0 )
        {
            final int sc = ( (AproxWorkflowException) error ).getStatus();
            rb = Response.status( Status.fromStatusCode( sc ) );
        }
        else
        {
            rb = Response.serverError();
        }

        if ( includeExplanation )
        {
            final String msg = formatEntity( error, message ).toString();
            rb.entity( msg )
              .type( MediaType.TEXT_PLAIN );
        }

        return rb.build();
    }

    public static CharSequence formatEntity( final Throwable error )
    {
        return formatEntity( error, null );
    }

    public static CharSequence formatEntity( final Throwable error, final String message )
    {
        final StringWriter sw = new StringWriter();
        if ( message != null )
        {
            sw.append( message );
            sw.append( "\nError was:\n\n" );
        }

        sw.append( error.getMessage() );

        final Throwable cause = error.getCause();
        if ( cause != null )
        {
            sw.append( "\n\n" );
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
