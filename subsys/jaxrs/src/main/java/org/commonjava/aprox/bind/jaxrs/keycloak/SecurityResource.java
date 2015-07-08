package org.commonjava.aprox.bind.jaxrs.keycloak;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.keycloak.rest.SecurityController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/api/security" )
public class SecurityResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private SecurityController controller;

    @Path( "/keycloak.json" )
    @GET
    public Response getKeycloakUiJson()
    {
        Response response = null;
        try
        {
            response = Response.ok( controller.getKeycloakUiJson() )
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to load client-side keycloak.json. Reason: %s", e.getMessage() ), e );
            response = formatResponse( e, true );
        }

        return response;
    }

    @Path( "/keycloak-init.js" )
    @GET
    public Response getKeycloakInit()
    {
        Response response = null;
        try
        {
            response = Response.ok( controller.getKeycloakInit() )
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to load keycloak-init.js. Reason: %s", e.getMessage() ), e );
            response = formatResponse( e, true );
        }

        return response;
    }

    @Path( "/keycloak.js" )
    @GET
    public Response getKeycloakJs()
    {
        Response response = null;
        try
        {
            response = Response.ok( controller.getKeycloakJs() )
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to load keycloak.js. Reason: %s", e.getMessage() ), e );
            response = formatResponse( e, true );
        }

        return response;
    }

}
