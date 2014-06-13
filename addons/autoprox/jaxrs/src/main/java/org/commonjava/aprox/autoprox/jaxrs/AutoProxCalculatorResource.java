package org.commonjava.aprox.autoprox.jaxrs;

import java.util.Collections;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.autoprox.rest.AutoProxCalculatorController;
import org.commonjava.aprox.autoprox.rest.dto.AutoProxCalculation;
import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.web.json.ser.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/autoprox/eval" )
public class AutoProxCalculatorResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @AproxData
    private JsonSerializer serializer;

    @Inject
    private AutoProxCalculatorController controller;

    @Path( "/remote/:name" )
    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response evalRemote( final @PathParam( "name" ) String name )
    {
        Response response;
        try
        {
            final AutoProxCalculation calc = controller.evalRemoteRepository( name );
            response =
                Response.ok( serializer.toString( calc == null ? Collections.singletonMap( "error",
                                                                                           "Nothing was created" )
                                             : calc ) )
                        .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to create demo RemoteRepository for: '%s'. Reason: %s", name,
                                         e.getMessage() ), e );
            response = AproxExceptionUtils.formatResponse( e );
        }

        return response;
    }

    @Path( "/hosted/:name" )
    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response evalHosted( final @PathParam( "name" ) String name )
    {
        Response response;
        try
        {
            final AutoProxCalculation calc = controller.evalHostedRepository( name );
            response =
                Response.ok( serializer.toString( calc == null ? Collections.singletonMap( "error",
                                                                                           "Nothing was created" )
                                             : calc ) )
                        .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to create demo HostedRepository for: '%s'. Reason: %s", name,
                                         e.getMessage() ), e );
            response = AproxExceptionUtils.formatResponse( e );
        }

        return response;
    }

    @Path( "/group/:name" )
    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response evalGroup( final @PathParam( "name" ) String name )
    {
        Response response;
        try
        {
            final AutoProxCalculation calc = controller.evalGroup( name );
            response =
                Response.ok( serializer.toString( calc == null ? Collections.singletonMap( "error",
                                                                                           "Nothing was created" )
                                             : calc ) )
                        .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to create demo Group for: '%s'. Reason: %s", name, e.getMessage() ), e );
            response = AproxExceptionUtils.formatResponse( e );
        }

        return response;
    }

}
