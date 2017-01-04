package org.commonjava.indy.bind.jaxrs;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Created by jdcasey on 1/3/17.
 * Based on: http://stackoverflow.com/questions/13857638/global-custom-exception-handler-in-resteasy
 */
@Provider
public class UnhandledIOExceptionHandler
        implements ExceptionMapper<IOException>//, RestProvider
{
    public Response toResponse( IOException exception )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.error( "Unhandled exception: " + exception.getMessage(), exception );

        return Response.status( Response.Status.INTERNAL_SERVER_ERROR )
                       .entity( ExceptionUtils.getFullStackTrace( exception ) )
                       .type( MediaType.TEXT_PLAIN )
                       .build();
    }
}
