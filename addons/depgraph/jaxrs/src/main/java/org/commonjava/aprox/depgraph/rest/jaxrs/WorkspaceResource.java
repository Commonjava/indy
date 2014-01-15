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
package org.commonjava.aprox.depgraph.rest.jaxrs;

import java.util.Set;

import javax.enterprise.context.RequestScoped;
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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.atlas.ident.version.VersionUtils;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.WorkspaceOps;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.model.Listing;
import org.commonjava.web.json.ser.JsonSerializer;
import org.commonjava.web.json.ser.ServletSerializerUtils;

@Path( "/depgraph/ws" )
@RequestScoped
public class WorkspaceResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private WorkspaceOps ops;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    @Path( "/{id}" )
    @DELETE
    public Response delete( @PathParam( "id" ) final String id )
    {
        try
        {
            if ( !ops.delete( id ) )
            {
                return Response.serverError()
                               .entity( "Delete failed" )
                               .build();
            }
        }
        catch ( final CartoDataException e )
        {
            return AproxExceptionUtils.formatResponse( e );
        }

        return Response.ok()
                       .build();
    }

    @Path( "/{id}" )
    @PUT
    @Produces( MediaType.APPLICATION_JSON )
    public Response createNamed( @PathParam( "id" ) final String id, @Context final UriInfo uriInfo )
    {
        try
        {
            final GraphWorkspace workspace = ops.create( id, new GraphWorkspaceConfiguration() );

            final String json = serializer.toString( workspace );

            return Response.created( uriInfo.getBaseUriBuilder()
                                            .path( getClass() )
                                            .path( workspace.getId() )
                                            .build() )
                           .entity( json )
                           .build();
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to create new workspace: ", e, e.getMessage() );
            return Response.serverError()
                           .build();
        }
    }

    @Path( "/new" )
    @POST
    @Produces( MediaType.APPLICATION_JSON )
    public Response create( @Context final UriInfo uriInfo )
    {
        try
        {
            final GraphWorkspace workspace = ops.create( new GraphWorkspaceConfiguration() );

            final String json = serializer.toString( workspace );

            return Response.created( uriInfo.getBaseUriBuilder()
                                            .path( getClass() )
                                            .path( workspace.getId() )
                                            .build() )
                           .entity( json )
                           .build();
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to create new workspace: ", e, e.getMessage() );
            return Response.serverError()
                           .build();
        }
    }

    @Path( "/new/from" )
    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response createFrom( @Context final UriInfo uriInfo, @Context final HttpServletRequest request )
    {
        final GraphWorkspaceConfiguration config = ServletSerializerUtils.fromRequestBody( request, serializer, GraphWorkspaceConfiguration.class );
        try
        {
            final GraphWorkspace workspace = ops.create( config );

            final String json = serializer.toString( workspace );

            return Response.created( uriInfo.getAbsolutePathBuilder()
                                            .path( getClass() )
                                            .path( workspace.getId() )
                                            .build() )
                           .entity( json )
                           .build();
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to create new workspace: ", e, e.getMessage() );
            return Response.serverError()
                           .build();
        }
    }

    @Path( "/{id}/select/{groupId}/{artifactId}/{newVersion}" )
    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response select( @PathParam( "id" ) final String id, @PathParam( "groupId" ) final String groupId,
                            @PathParam( "artifactId" ) final String artifactId, @PathParam( "newVersion" ) final String newVersion,
                            @QueryParam( "for" ) final String oldVersion, @Context final UriInfo uriInfo )
    {
        Response response = Response.notModified()
                                    .build();

        GraphWorkspace ws;
        boolean modified = false;
        ProjectRef pr = null;
        try
        {
            final SingleVersion ver = VersionUtils.createSingleVersion( newVersion );

            ws = ops.get( id );
            if ( oldVersion == null )
            {
                pr = new ProjectRef( groupId, artifactId );
                modified = ws.selectVersionForAll( pr, new ProjectVersionRef( pr, ver ) );
            }
            else
            {
                final ProjectVersionRef orig = new ProjectVersionRef( groupId, artifactId, oldVersion );
                final ProjectVersionRef selected = ws.selectVersion( orig, orig.selectVersion( ver ) );

                modified = selected.equals( orig );
                pr = orig;
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to load workspace: %s. Reason: %s", e, id, e.getMessage() );
            return Response.serverError()
                           .build();
        }
        catch ( final GraphDriverException e )
        {
            logger.error( "Failed to select: %s for: %s. Reason: %s", e, newVersion, pr, e.getMessage() );
            return Response.serverError()
                           .build();
        }

        if ( modified )
        {
            final String json = serializer.toString( ws );
            response = Response.ok( json )
                               .location( uriInfo.getAbsolutePathBuilder()
                                                 .path( getClass() )
                                                 .path( ws.getId() )
                                                 .build() )
                               .build();
        }

        // detach from the threadlocal...
        ws.detach();

        return response;
    }

    @Path( "/{id}" )
    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response get( @PathParam( "id" ) final String id )
    {
        Response response = Response.notModified()
                                    .build();

        GraphWorkspace ws;
        try
        {
            ws = ops.get( id );
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to load workspace: %s. Reason: %s", e, id, e.getMessage() );
            return Response.serverError()
                           .build();
        }

        if ( ws == null )
        {
            logger.debug( "No workspace found for: %s", id );
            return Response.status( Status.NOT_FOUND )
                           .entity( "No workspace for id: " + id )
                           .build();
        }

        final String json = serializer.toString( ws );
        response = Response.ok( json )
                           .build();

        // detach from the threadlocal...
        ws.detach();

        return response;
    }

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response list()
    {
        logger.info( "Listing workspaces..." );
        Response response = Response.notModified()
                                    .build();

        final Set<GraphWorkspace> ws = ops.list();

        if ( ws == null || ws.isEmpty() )
        {
            logger.info( "No workspaces found." );
            return Response.status( Status.NO_CONTENT )
                           .entity( "No workspaces found." )
                           .build();
        }

        final String json = serializer.toString( new Listing<GraphWorkspace>( ws ) );
        response = Response.ok( json )
                           .build();

        return response;
    }

    @Path( "/{id}/source/{source}" )
    @PUT
    @Produces( MediaType.APPLICATION_JSON )
    public Response addSource( @PathParam( "id" ) final String id, @PathParam( "source" ) final String source, @Context final UriInfo uriInfo )
    {
        Response response = Response.notModified()
                                    .build();

        GraphWorkspace ws;
        try
        {
            ws = ops.get( id );

            if ( ws == null )
            {
                logger.debug( "No workspace found for: %s", id );
                return Response.status( Status.NOT_FOUND )
                               .entity( "No workspace for id: " + id )
                               .build();
            }

            boolean modified = false;

            if ( source != null )
            {
                ops.addSource( source, ws );
                modified = true;
            }

            if ( modified )
            {
                final String json = serializer.toString( ws );
                response = Response.ok( json )
                                   .location( uriInfo.getAbsolutePathBuilder()
                                                     .path( getClass() )
                                                     .path( ws.getId() )
                                                     .build() )
                                   .build();
            }

            // detach from the threadlocal...
            ws.detach();
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to load workspace: %s. Reason: %s", e, id, e.getMessage() );
            return Response.serverError()
                           .build();
        }

        return response;
    }

    @Path( "/{id}/profile/{profile}" )
    @PUT
    @Produces( MediaType.APPLICATION_JSON )
    public Response addPomLocation( @PathParam( "id" ) final String id, @PathParam( "profile" ) final String profile, @Context final UriInfo uriInfo )
    {
        Response response = Response.notModified()
                                    .build();

        GraphWorkspace ws;
        try
        {
            ws = ops.get( id );
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to load workspace: %s. Reason: %s", e, id, e.getMessage() );
            return Response.serverError()
                           .build();
        }

        if ( ws == null )
        {
            logger.debug( "No workspace found for: %s", id );
            return Response.status( Status.NOT_FOUND )
                           .entity( "No workspace for id: " + id )
                           .build();
        }

        ops.addProfile( profile, ws );
        final String json = serializer.toString( ws );
        response = Response.ok( json )
                           .location( uriInfo.getAbsolutePathBuilder()
                                             .path( getClass() )
                                             .path( ws.getId() )
                                             .build() )
                           .build();

        // detach from the threadlocal...
        ws.detach();

        return response;
    }
}
