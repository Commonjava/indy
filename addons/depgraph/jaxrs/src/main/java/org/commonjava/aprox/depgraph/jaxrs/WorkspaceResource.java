/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.depgraph.jaxrs;

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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.bind.jaxrs.util.JaxRsUriFormatter;
import org.commonjava.aprox.core.dto.CreationDTO;
import org.commonjava.aprox.depgraph.rest.WorkspaceController;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/depgraph/ws" )
@ApplicationScoped
public class WorkspaceResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private WorkspaceController controller;

    @Path( "/{id}" )
    @DELETE
    public Response delete( @PathParam( "id" ) final String id )
    {
        try
        {
            controller.delete( id );
            return Response.ok()
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @Path( "/{id}" )
    @PUT
    @Produces( MediaType.APPLICATION_JSON )
    public Response createNamed( @PathParam( "id" ) final String id, @Context final UriInfo uriInfo )
    {
        try
        {
            final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            final CreationDTO dto = controller.createNamed( id, uriBuilder.path( getClass() )
                                                                          .build()
                                                                          .toString(), new JaxRsUriFormatter( uriBuilder ) );
            return dto == null ? Response.notModified()
                                         .build() : Response.ok( dto.getJsonResponse() )
                                                            .type( MediaType.APPLICATION_JSON )
                                                            .location( dto.getUri() )
                                                            .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @Path( "/new" )
    @POST
    @Produces( MediaType.APPLICATION_JSON )
    public Response create( @Context final UriInfo uriInfo )
    {
        try
        {
            final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            final CreationDTO dto = controller.create( uriBuilder.path( getClass() )
                                                                 .build()
                                                                 .toString(), new JaxRsUriFormatter( uriBuilder ) );
            return dto == null ? Response.notModified()
                                         .build() : Response.ok( dto.getJsonResponse() )
                                                            .type( MediaType.APPLICATION_JSON )
                                                            .location( dto.getUri() )
                                                            .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @Path( "/new/from" )
    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response createFrom( @Context final UriInfo uriInfo, @Context final HttpServletRequest request )
    {
        try
        {
            final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            final CreationDTO dto =
                controller.createFrom( uriBuilder.path( getClass() )
                                                 .build()
                                                 .toString(), new JaxRsUriFormatter( uriBuilder ), request.getInputStream(),
                                       request.getCharacterEncoding() );
            return dto == null ? Response.notModified()
                                         .build() : Response.ok( dto.getJsonResponse() )
                                                            .type( MediaType.APPLICATION_JSON )
                                                            .location( dto.getUri() )
                                                            .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to retrieve servlet request input stream: {}", e, e.getMessage() );
            return AproxExceptionUtils.formatResponse( ApplicationStatus.BAD_REQUEST, e );
        }
    }

    @Path( "/{id}/select/{groupId}/{artifactId}/{newVersion}" )
    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response select( @PathParam( "id" ) final String id, @PathParam( "groupId" ) final String groupId,
                            @PathParam( "artifactId" ) final String artifactId, @PathParam( "newVersion" ) final String newVersion,
                            @QueryParam( "for" ) final String oldVersion, @Context final UriInfo uriInfo )
    {
        try
        {
            final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            final boolean modified = controller.select( id, groupId, artifactId, newVersion, oldVersion, new JaxRsUriFormatter( uriBuilder ) );
            return modified ? Response.ok()
                                      .build() : Response.notModified()
                                                         .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @Path( "/{id}" )
    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response get( @PathParam( "id" ) final String id )
    {
        try
        {
            final String json = controller.get( id );
            return json == null ? Response.status( ApplicationStatus.NOT_FOUND.code() )
                                          .build() : Response.ok( json )
                                                             .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response list()
    {
        final String json = controller.list();
        return json == null ? Response.noContent()
                                      .build() : Response.ok( json )
                                                         .build();
    }

    @Path( "/{id}/source/{source}" )
    @PUT
    @Produces( MediaType.APPLICATION_JSON )
    public Response addSource( @PathParam( "id" ) final String id, @PathParam( "source" ) final String source, @Context final UriInfo uriInfo )
    {
        try
        {
            final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            final boolean modified = controller.addSource( id, source, new JaxRsUriFormatter( uriBuilder ) );
            return modified ? Response.ok()
                                      .build() : Response.notModified()
                                                         .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @Path( "/{id}/profile/{profile}" )
    @PUT
    @Produces( MediaType.APPLICATION_JSON )
    public Response addPomLocation( @PathParam( "id" ) final String id, @PathParam( "profile" ) final String profile, @Context final UriInfo uriInfo )
    {
        try
        {
            final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            final boolean modified = controller.addPomLocation( id, profile, new JaxRsUriFormatter( uriBuilder ) );
            return modified ? Response.ok()
                                      .build() : Response.notModified()
                                                         .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }
}
