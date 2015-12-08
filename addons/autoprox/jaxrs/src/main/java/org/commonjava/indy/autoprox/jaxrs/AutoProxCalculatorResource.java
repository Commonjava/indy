/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.autoprox.jaxrs;

import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatResponse;

import java.util.Collections;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.autoprox.conf.AutoProxConfig;
import org.commonjava.indy.autoprox.rest.AutoProxCalculatorController;
import org.commonjava.indy.autoprox.rest.dto.AutoProxCalculation;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.util.ApplicationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path( "/api/autoprox/eval" )
public class AutoProxCalculatorResource
    implements IndyResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ObjectMapper serializer;

    @Inject
    private AutoProxCalculatorController controller;

    @Inject
    private AutoProxConfig config;

    @Path( "/remote/{name}" )
    @GET
    @Consumes( ApplicationContent.application_json )
    public Response evalRemote( final @PathParam( "name" ) String remoteName )
    {
        Response response = checkEnabled();
        if ( response != null )
        {
            return response;
        }

        try
        {
            final AutoProxCalculation calc = controller.evalRemoteRepository( remoteName );

            response =
                formatOkResponseWithJsonEntity( serializer.writeValueAsString( calc == null ? Collections.singletonMap( "error",
                                                                                                          "Nothing was created" )
                                                            : calc ) );
        }
        catch ( final IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to create demo RemoteRepository for: '%s'. Reason: %s", remoteName,
                                         e.getMessage() ), e );
            response = formatResponse( e );
        }
        catch ( final JsonProcessingException e )
        {
            logger.error( String.format( "Failed to create demo RemoteRepository for: '%s'. Reason: %s", remoteName,
                                         e.getMessage() ), e );
            response = formatResponse( e );
        }

        return response;
    }

    private Response checkEnabled()
    {
        if ( !config.isEnabled() )
        {
            return Response.status( Response.Status.BAD_REQUEST ).entity( "Autoprox is disabled." ).build();
        }

        return null;
    }

    @Path( "/hosted/{name}" )
    @GET
    @Consumes( ApplicationContent.application_json )
    public Response evalHosted( final @PathParam( "name" ) String hostedName )
    {
        Response response = checkEnabled();
        if ( response != null )
        {
            return response;
        }

        try
        {
            final AutoProxCalculation calc = controller.evalHostedRepository( hostedName );

            response =
                formatOkResponseWithJsonEntity( serializer.writeValueAsString( calc == null ? Collections.singletonMap( "error",
                                                                                                          "Nothing was created" )
                                                            : calc ) );
        }
        catch ( final IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to create demo HostedRepository for: '%s'. Reason: %s", hostedName,
                                         e.getMessage() ), e );
            response = formatResponse( e );
        }
        catch ( final JsonProcessingException e )
        {
            logger.error( String.format( "Failed to create demo HostedRepository for: '%s'. Reason: %s", hostedName,
                                         e.getMessage() ), e );
            response = formatResponse( e );
        }
        return response;
    }

    @Path( "/group/{name}" )
    @GET
    @Consumes( ApplicationContent.application_json )
    public Response evalGroup( final @PathParam( "name" ) String groupName )
    {
        Response response = checkEnabled();
        if ( response != null )
        {
            return response;
        }

        try
        {
            final AutoProxCalculation calc = controller.evalGroup( groupName );

            response =
                formatOkResponseWithJsonEntity( serializer.writeValueAsString( calc == null ? Collections.singletonMap( "error",
                                                                                                          "Nothing was created" )
                                                            : calc ) );
        }
        catch ( final IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to create demo Group for: '%s'. Reason: %s", groupName, e.getMessage() ),
                          e );
            response = formatResponse( e );
        }
        catch ( final JsonProcessingException e )
        {
            logger.error( String.format( "Failed to create demo Group for: '%s'. Reason: %s", groupName, e.getMessage() ),
                          e );
            response = formatResponse( e );
        }

        return response;
    }

}
