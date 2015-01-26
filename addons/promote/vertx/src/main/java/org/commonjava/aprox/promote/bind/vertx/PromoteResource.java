package org.commonjava.aprox.promote.bind.vertx;

import java.io.IOException;

import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.promote.data.PromotionException;
import org.commonjava.aprox.promote.data.PromotionManager;
import org.commonjava.aprox.promote.model.PromoteRequest;
import org.commonjava.aprox.promote.model.PromoteResult;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.util.Respond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Handles( "/promotion" )
public class PromoteResource
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private PromotionManager manager;

    @Inject
    private ObjectMapper mapper;

    @Route( path = "/promote", method = Method.POST, contentType = ApplicationContent.application_json )
    public void promote( final Buffer buffer, final HttpServerRequest request )
    {
        final String json = buffer.getString( 0, buffer.length() );
        PromoteRequest req;
        try
        {
            req = mapper.readValue( json, PromoteRequest.class );
        }
        catch ( final IOException e )
        {
            final String message = "Failed to read PromoteRequest from JSON:\n" + json;
            logger.error( message, e );
            Respond.to( request )
                   .serverError( e, message, true )
                   .send();
            return;
        }

        try
        {
            final PromoteResult result = manager.promote( req );

            // TODO: Amend response status code based on presence of error? This would have consequences for client API...
            Respond.to( request )
                   .jsonEntity( result, mapper )
                   .ok()
                   .send();
        }
        catch ( PromotionException | AproxWorkflowException | JsonProcessingException e )
        {
            logger.error( e.getMessage(), e );
            Respond.to( request )
                   .serverError( e, true )
                   .send();
        }
    }

    @Route( path = "/resume", method = Method.POST, contentType = ApplicationContent.application_json )
    public void resume( final Buffer buffer, final HttpServerRequest request )
    {
        final String json = buffer.getString( 0, buffer.length() );
        PromoteResult result;
        try
        {
            result = mapper.readValue( json, PromoteResult.class );
        }
        catch ( final IOException e )
        {
            final String message = "Failed to read PromoteResult from JSON:\n" + json;
            logger.error( message, e );
            Respond.to( request )
                   .serverError( e, message, true )
                   .send();
            return;
        }

        try
        {
            result = manager.resume( result );

            // TODO: Amend response status code based on presence of error? This would have consequences for client API...
            Respond.to( request )
                   .jsonEntity( result, mapper )
                   .ok()
                   .send();
        }
        catch ( PromotionException | AproxWorkflowException | JsonProcessingException e )
        {
            logger.error( e.getMessage(), e );
            Respond.to( request )
                   .serverError( e, true )
                   .send();
        }
    }

    @Route( path = "/rollback", method = Method.POST, contentType = ApplicationContent.application_json )
    public void rollback( final Buffer buffer, final HttpServerRequest request )
    {
        final String json = buffer.getString( 0, buffer.length() );
        PromoteResult result;
        try
        {
            result = mapper.readValue( json, PromoteResult.class );
        }
        catch ( final IOException e )
        {
            final String message = "Failed to read PromoteResult from JSON:\n" + json;
            logger.error( message, e );
            Respond.to( request )
                   .serverError( e, message, true )
                   .send();
            return;
        }

        try
        {
            result = manager.rollback( result );

            // TODO: Amend response status code based on presence of error? This would have consequences for client API...
            Respond.to( request )
                   .jsonEntity( result, mapper )
                   .ok()
                   .send();
        }
        catch ( PromotionException | AproxWorkflowException | JsonProcessingException e )
        {
            logger.error( e.getMessage(), e );
            Respond.to( request )
                   .serverError( e, true )
                   .send();
        }
    }

}
