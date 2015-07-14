package org.commonjava.aprox.bind.jaxrs.keycloak;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.keycloak.rest.SecurityController;
import org.commonjava.aprox.util.ApplicationHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/api/security" )
public class SecurityResource
    implements AproxResources
{

    private static final String DISABLED_MESSAGE = "Keycloak is disabled";

    private static final String NO_CACHE = "no-store, no-cache, must-revalidate, max-age=0";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private SecurityController controller;

    @Path( "/keycloak.json" )
    @GET
    public Response getKeycloakUiJson()
    {
        logger.debug( "Retrieving Keycloak UI JSON file..." );
        Response response = null;
        try
        {
            final String content = controller.getKeycloakUiJson();
            if ( content == null )
            {
                response = Response.status( Status.NOT_ACCEPTABLE )
                                   .entity( DISABLED_MESSAGE )
                                   .header( ApplicationHeader.cache_control.key(), NO_CACHE )
                                   .build();
            }
            else
            {
                response = Response.ok( content )
                                   .header( ApplicationHeader.cache_control.key(), NO_CACHE )
                                   .build();
            }
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
        logger.debug( "Retrieving Keycloak UI-init Javascript file..." );
        Response response = null;
        try
        {
            response = Response.ok( controller.getKeycloakInit() )
                               .header( ApplicationHeader.cache_control.key(), NO_CACHE )
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
        logger.debug( "Retrieving Keycloak Javascript adapter..." );
        Response response = null;
        try
        {
            final String url = controller.getKeycloakJs();
            if ( url == null )
            {
                response = Response.ok( "/* " + DISABLED_MESSAGE + "; loading of keycloak.js blocked. */" )
                                   .header( ApplicationHeader.cache_control.key(), NO_CACHE )
                                   .build();
            }
            else
            {
                response = Response.temporaryRedirect( new URI( url ) )
                                   .build();
            }
        }
        catch ( final AproxWorkflowException | URISyntaxException e )
        {
            logger.error( String.format( "Failed to load keycloak.js. Reason: %s", e.getMessage() ), e );
            response = formatResponse( e, true );
        }

        return response;
    }

}
