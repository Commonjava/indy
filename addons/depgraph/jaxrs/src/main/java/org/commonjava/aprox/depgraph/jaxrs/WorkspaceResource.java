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
package org.commonjava.aprox.depgraph.jaxrs;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatCreatedResponse;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.util.ApplicationContent.application_json;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.bind.jaxrs.util.JaxRsUriFormatter;
import org.commonjava.aprox.depgraph.rest.WorkspaceController;
import org.commonjava.aprox.model.core.dto.CreationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/api/depgraph/ws" )
@ApplicationScoped
public class WorkspaceResource
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private WorkspaceController controller;

    @Path( "/{wsid}" )
    @DELETE
    public Response delete( final @PathParam( "wsid" ) String id )
    {
        Response response;
        try
        {
            controller.delete( id );
            response = Response.ok()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/{wsid}" )
    @PUT
    @Consumes( application_json )
    public Response createNamed( final @PathParam( "wsid" ) String id, final @Context UriInfo uriInfo )
    {
        Response response;
        try
        {
            final String baseUrl = uriInfo.getAbsolutePathBuilder()
                                          .path( getClass() )
                                          .toString();
            final CreationDTO dto = controller.createNamed( id, baseUrl, new JaxRsUriFormatter() );

            if ( dto != null )
            {
                response = formatCreatedResponse( baseUrl, dto );
            }
            else
            {
                response = Response.status( Status.NOT_MODIFIED )
                                   .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }

        return response;
    }

    @Path( "/new" )
    @POST
    @Produces( application_json )
    public Response create( final @Context UriInfo uriInfo )
    {
        Response response;
        try
        {
            final String baseUri = uriInfo.getAbsolutePathBuilder()
                                          .path( getClass() )
                                          .toString();

            final CreationDTO dto = controller.create( baseUri, new JaxRsUriFormatter() );
            if ( dto != null )
            {
                response = formatCreatedResponse( baseUri, dto );
            }
            else
            {
                response = Response.status( Status.NOT_MODIFIED )
                                   .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }

        return response;
    }

    @Path( "/new/from" )
    @POST
    @Produces( application_json )
    public Response createFrom( final @Context UriInfo uriInfo, @Context final HttpServletRequest request )
    {
        Response response;
        try
        {
            final String baseUri = uriInfo.getAbsolutePathBuilder()
                                          .path( getClass() )
                                          .toString();
            // FIXME Figure out the character encoding!
            final CreationDTO dto =
                controller.createFrom( baseUri, new JaxRsUriFormatter(), request.getInputStream(),
                                       request.getCharacterEncoding() );

            if ( dto != null )
            {
                response = formatCreatedResponse( baseUri, dto );
            }
            else
            {
                response = Response.status( Status.NOT_MODIFIED )
                                   .build();
            }
        }
        catch ( final AproxWorkflowException | IOException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }

        return response;
    }

    @Path( "/{wsid}" )
    @GET
    @Produces( application_json )
    public Response get( final @PathParam( "wsid" ) String id )
    {
        Response response;
        try
        {
            final String json = controller.get( id );
            if ( json == null )
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }
            else
            {
                response = formatOkResponseWithJsonEntity( json );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @GET
    @Produces( application_json )
    public Response list()
    {
        Response response = null;

        String json = null;
        try
        {
            json = controller.list();
        }
        catch ( final AproxWorkflowException e )
        {
            response = formatResponse( e, true );
        }

        if ( response == null )
        {
            if ( json == null )
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }
            else
            {
                response = formatOkResponseWithJsonEntity( json );
            }
        }

        return response;
    }
}
