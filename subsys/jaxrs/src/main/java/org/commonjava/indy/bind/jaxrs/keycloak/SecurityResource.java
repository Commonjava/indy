/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.bind.jaxrs.keycloak;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.REST;
import org.commonjava.indy.bind.jaxrs.util.ResponseHelper;
import org.commonjava.indy.subsys.keycloak.rest.SecurityController;
import org.commonjava.indy.util.ApplicationHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.net.URISyntaxException;

@Api( "Security Infrastructure" )
@Path( "/api/security" )
@REST
public class SecurityResource
        implements IndyResources
{

    private static final String DISABLED_MESSAGE = "Keycloak is disabled";

    private static final String NO_CACHE = "no-store, no-cache, must-revalidate, max-age=0";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private SecurityController controller;

    @Inject
    private ResponseHelper responseHelper;

    @ApiOperation( "Retrieve the keycloak JSON configuration (for use by the UI)" )
    @ApiResponses( { @ApiResponse( code = 400, message = "Keycloak is disabled" ),
                           @ApiResponse( code = 200, message = "File retrieval successful" ) } )
    @Path( "/keycloak.json" )
    @Produces( "application/json" )
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
                response = Response.status( Status.BAD_REQUEST )
                                   .entity( DISABLED_MESSAGE )
                                   .header( ApplicationHeader.cache_control.key(), NO_CACHE )
                                   .build();
            }
            else
            {
                response = Response.ok( content ).header( ApplicationHeader.cache_control.key(), NO_CACHE ).build();
            }
        }
        catch ( final IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to load client-side keycloak.json. Reason: %s", e.getMessage() ), e );
            response = responseHelper.formatResponse( e );
        }

        return response;
    }

    @ApiOperation( "Retrieve the keycloak init Javascript (for use by the UI)" )
    @ApiResponse( code = 200, message = "Always return 200 whether Keycloak is disabled or not" )
    @Path( "/keycloak-init.js" )
    @Produces( "text/javascript" )
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
        catch ( final IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to load keycloak-init.js. Reason: %s", e.getMessage() ), e );
            response = responseHelper.formatResponse( e );
        }

        return response;
    }

    @ApiOperation( "Retrieve the keycloak Javascript adapter (for use by the UI)" )
    @ApiResponses(
            { @ApiResponse( code = 200, message = "Keycloak is disabled, return a Javascript comment to this effect." ),
                    @ApiResponse( code = 307, message = "Redirect to keycloak server to load Javascript adapter." ) } )
    @Path( "/keycloak.js" )
    @Produces( "text/javascript" )
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
                response = Response.temporaryRedirect( new URI( url ) ).build();
            }
        }
        catch ( final IndyWorkflowException | URISyntaxException e )
        {
            logger.error( String.format( "Failed to load keycloak.js. Reason: %s", e.getMessage() ), e );
            response = responseHelper.formatResponse( e );
        }

        return response;
    }

}
