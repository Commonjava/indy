package org.commonjava.aprox.bind.vertx.util;

import org.commonjava.aprox.core.util.UriFormatter;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationHeader;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

public final class ResponseUtils
{

    private ResponseUtils()
    {
    }

    public static void setStatus( final ApplicationStatus status, final HttpServerRequest request )
    {
        request.response()
               .setStatusCode( status.code() )
               .setStatusMessage( status.message() );
    }

    public static void setStatus( final ApplicationStatus status, final HttpServerResponse response )
    {
        response.setStatusCode( status.code() )
                .setStatusMessage( status.message() );
    }

    public static void formatRedirect( final HttpServerRequest request, final String url )
    {
        setStatus( ApplicationStatus.FOUND, request );
        request.response()
               .putHeader( ApplicationHeader.uri.key(), url );
    }

    public static void formatCreatedResponse( final HttpServerRequest request, final UriFormatter uriFormatter, final String basePath,
                                              final String... params )
    {
        final String location = uriFormatter.formatAbsolutePathTo( basePath, params );

        request.response()
               .setStatusCode( ApplicationStatus.CREATED.code() )
               .setStatusMessage( ApplicationStatus.CREATED.message() )
               .putHeader( ApplicationHeader.location.key(), location );
    }

    public static void formatOkResponseWithEntity( final HttpServerRequest request, final String json )
    {
        request.response()
               .setStatusCode( ApplicationStatus.OK.code() )
               .setStatusMessage( ApplicationStatus.OK.message() )
               .write( json );
    }

    public static void formatBadRequestResponse( final HttpServerRequest request, final String error )
    {
        request.response()
               .setStatusCode( ApplicationStatus.BAD_REQUEST.code() )
               .setStatusMessage( ApplicationStatus.BAD_REQUEST.message() )
               .write( "{\"error\": \"" + error + "\"}" );
    }

    public static void formatResponse( final AproxWorkflowException error, final HttpServerResponse response )
    {
        formatResponse( error, response, true );
    }

    public static void formatResponse( final AproxWorkflowException error, final HttpServerResponse response, final boolean includeExplanation )
    {
        if ( error.getStatus() > 0 )
        {
            response.setStatusCode( error.getStatus() );
        }
        else
        {
            response.setStatusCode( ApplicationStatus.SERVER_ERROR.code() )
                    .setStatusMessage( ApplicationStatus.SERVER_ERROR.message() );
        }

        if ( includeExplanation )
        {
            response.write( error.formatEntity()
                                 .toString() );
        }
    }

    public static void formatResponse( final Throwable e, final HttpServerResponse response )
    {
        formatResponse( e, response, true );
    }

    public static void formatResponse( final Throwable error, final HttpServerResponse response, final boolean includeExplanation )
    {
        response.setStatusCode( ApplicationStatus.SERVER_ERROR.code() )
                .setStatusMessage( ApplicationStatus.SERVER_ERROR.message() );

        if ( includeExplanation )
        {
            response.write( error.getMessage() );
        }
    }

}
