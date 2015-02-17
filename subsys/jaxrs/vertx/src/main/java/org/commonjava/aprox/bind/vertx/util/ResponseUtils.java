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
package org.commonjava.aprox.bind.vertx.util;

import static org.commonjava.vertx.vabr.types.BuiltInParam._classContextUrl;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.model.core.dto.CreationDTO;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.UriFormatter;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

public final class ResponseUtils
{

    private ResponseUtils()
    {
    }

    public static void status( final ApplicationStatus status, final HttpServerRequest request )
    {
        _setStatus( status, request ).end();
    }

    private static HttpServerResponse _setStatus( final ApplicationStatus status, final HttpServerRequest request )
    {
        request.resume()
               .response()
               .setStatusCode( status.code() )
               .setStatusMessage( status.message() );

        return request.response();
    }

    public static void formatRedirect( final HttpServerRequest request, final String url )
    {
        _setStatus( ApplicationStatus.MOVED_PERMANENTLY, request );
        request.resume()
               .response()
               .putHeader( ApplicationHeader.uri.key(), url )
               .putHeader( ApplicationHeader.location.key(), url )
               .end();
    }

    public static HttpServerResponse formatCreatedResponse( final HttpServerRequest request,
                                                            final UriFormatter uriFormatter, final String... params )
    {
        final String baseUri = request.params()
                                      .get( _classContextUrl.key() );

        final String location = uriFormatter.formatAbsolutePathTo( baseUri, params );

        request.resume()
               .response()
               .putHeader( ApplicationHeader.location.key(), location )
               .setStatusCode( ApplicationStatus.CREATED.code() )
               .setStatusMessage( ApplicationStatus.CREATED.message() )
               .end();

        return request.response();
    }

    public static void formatCreatedResponse( final HttpServerRequest request, final CreationDTO dto )
    {
        request.resume()
               .response()
               .setStatusCode( ApplicationStatus.CREATED.code() )
               .setStatusMessage( ApplicationStatus.CREATED.message() )
               .putHeader( ApplicationHeader.location.key(), dto.getUri()
                                                                .toString() );

        final String json = dto.getJsonResponse();
        if ( json != null )
        {
            request.response()
                   .putHeader( ApplicationHeader.content_type.key(), ApplicationContent.application_json )
                   .putHeader( ApplicationHeader.content_length.key(), Integer.toString( json.length() ) );

            request.response()
                   .end( json );
        }
        else
        {
            request.response()
                   .end();
        }
    }

    public static void formatOkResponseWithJsonEntity( final HttpServerRequest request, final String json )
    {
        formatOkResponseWithEntity( request, json, ApplicationContent.application_json );
    }

    public static void formatOkResponseWithEntity( final HttpServerRequest request, final String output, final String contentType )
    {
        request.resume()
               .response()
               .setStatusCode( ApplicationStatus.OK.code() )
               .setStatusMessage( ApplicationStatus.OK.message() )
               .putHeader( ApplicationHeader.content_type.key(), contentType )
               .putHeader( ApplicationHeader.content_length.key(), Integer.toString( output.length() ) )
               .write( output )
               .end();
    }

    public static void formatBadRequestResponse( final HttpServerRequest request, final String error )
    {
        final String msg = "{\"error\": \"" + error + "\"}\n";
        request.resume()
               .response()
               .setStatusCode( ApplicationStatus.BAD_REQUEST.code() )
               .setStatusMessage( ApplicationStatus.BAD_REQUEST.message() )
               .putHeader( ApplicationHeader.content_length.key(), Integer.toString( msg.length() ) )
               .write( msg )
               .end();
    }

    public static void formatResponse( final Throwable error, final HttpServerRequest request )
    {
        formatResponse( null, error, true, request );
    }

    public static void formatResponse( final ApplicationStatus status, final Throwable error, final HttpServerRequest request )
    {
        formatResponse( status, error, true, request );
    }

    public static void formatResponse( final Throwable error, final boolean includeExplanation, final HttpServerRequest request )
    {
        formatResponse( null, error, includeExplanation, request );
    }

    public static void formatResponse( final ApplicationStatus status, final Throwable error, final boolean includeExplanation,
                                       final HttpServerRequest request )
    {
        final HttpServerResponse response = request.resume()
                                                   .response();

        if ( status != null )
        {
            response.setStatusCode( status.code() )
                    .setStatusMessage( status.message() );
        }
        else if ( ( error instanceof AproxWorkflowException ) && ( (AproxWorkflowException) error ).getStatus() > 0 )
        {
            final int sc = ( (AproxWorkflowException) error ).getStatus();
            final ApplicationStatus stat = ApplicationStatus.getStatus( sc );
            if ( stat != null )
            {
                response.setStatusCode( sc )
                        .setStatusMessage( stat.message() );
            }
            else
            {
                response.setStatusCode( sc );
            }
        }
        else
        {
            response.setStatusCode( ApplicationStatus.SERVER_ERROR.code() )
                    .setStatusMessage( ApplicationStatus.SERVER_ERROR.message() );
        }

        if ( includeExplanation )
        {
            final String msg = formatEntity( error ).toString();
            response.putHeader( ApplicationHeader.content_length.key(), Integer.toString( msg.length() ) )
                    .write( msg )
                    .end();
        }
        else
        {
            response.end();
        }
    }

    public static CharSequence formatEntity( final Throwable error )
    {
        final StringWriter sw = new StringWriter();
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

    public static void markDeprecated( final HttpServerRequest request, final String alt )
    {
        request.response()
               .putHeader( ApplicationHeader.deprecated.key(), alt );
    }

}
