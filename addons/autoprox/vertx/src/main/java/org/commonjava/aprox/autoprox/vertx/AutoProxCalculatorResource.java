package org.commonjava.aprox.autoprox.vertx;

import static org.commonjava.aprox.bind.vertx.util.PathParam.name;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;

import java.util.Collections;

import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.autoprox.rest.AutoProxCalculatorController;
import org.commonjava.aprox.autoprox.rest.dto.AutoProxCalculation;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Handles( "/autoprox/eval" )
public class AutoProxCalculatorResource
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ObjectMapper serializer;

    @Inject
    private AutoProxCalculatorController controller;

    @Routes( { @Route( path = "/remote/:name", method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void evalRemote( final Buffer buffer, final HttpServerRequest request )
    {
        final String remoteName = request.params()
                                         .get( name.key() );
        try
        {
            final AutoProxCalculation calc = controller.evalRemoteRepository( remoteName );

            formatOkResponseWithJsonEntity( request,
                                            serializer.writeValueAsString( calc == null ? Collections.singletonMap( "error",
                                                                                                          "Nothing was created" )
                                                            : calc ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to create demo RemoteRepository for: '%s'. Reason: %s", remoteName,
                                         e.getMessage() ), e );
            formatResponse( e, request );
        }
        catch ( final JsonProcessingException e )
        {
            logger.error( String.format( "Failed to create demo RemoteRepository for: '%s'. Reason: %s", remoteName,
                                         e.getMessage() ), e );
            formatResponse( e, request );
        }
    }

    @Routes( { @Route( path = "/hosted/:name", method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void evalHosted( final Buffer buffer, final HttpServerRequest request )
    {
        final String hostedName = request.params()
                                         .get( name.key() );

        try
        {
            final AutoProxCalculation calc = controller.evalHostedRepository( hostedName );

            formatOkResponseWithJsonEntity( request,
                                            serializer.writeValueAsString( calc == null ? Collections.singletonMap( "error",
                                                                                                          "Nothing was created" )
                                                            : calc ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to create demo HostedRepository for: '%s'. Reason: %s", hostedName,
                                         e.getMessage() ), e );
            formatResponse( e, request );
        }
        catch ( final JsonProcessingException e )
        {
            logger.error( String.format( "Failed to create demo HostedRepository for: '%s'. Reason: %s", hostedName,
                                         e.getMessage() ), e );
            formatResponse( e, request );
        }
    }

    @Routes( { @Route( path = "/group/:name", method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void evalGroup( final Buffer buffer, final HttpServerRequest request )
    {
        final String groupName = request.params()
                                        .get( name.key() );

        try
        {
            final AutoProxCalculation calc = controller.evalGroup( groupName );

            formatOkResponseWithJsonEntity( request,
                                            serializer.writeValueAsString( calc == null ? Collections.singletonMap( "error",
                                                                                                          "Nothing was created" )
                                                            : calc ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to create demo Group for: '%s'. Reason: %s", groupName, e.getMessage() ),
                          e );
            formatResponse( e, request );
        }
        catch ( final JsonProcessingException e )
        {
            logger.error( String.format( "Failed to create demo Group for: '%s'. Reason: %s", groupName, e.getMessage() ),
                          e );
            formatResponse( e, request );
        }
    }

}
