package org.commonjava.aprox.depgraph.rest;

import static org.commonjava.maven.atlas.effective.util.RelationshipUtils.POM_ROOT_URI;
import static org.commonjava.maven.atlas.effective.util.RelationshipUtils.profileLocation;

import java.net.URI;
import java.util.HashSet;
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

import org.commonjava.maven.atlas.common.ref.ProjectRef;
import org.commonjava.maven.atlas.common.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.common.version.SingleVersion;
import org.commonjava.maven.atlas.common.version.VersionUtils;
import org.commonjava.maven.atlas.effective.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.effective.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.spi.GraphDriverException;
import org.commonjava.tensor.data.CartoDataException;
import org.commonjava.tensor.data.CartoDataManager;
import org.commonjava.tensor.discover.DiscoverySourceManager;
import org.commonjava.tensor.inject.TensorData;
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
    private CartoDataManager dataManager;

    @Inject
    private DiscoverySourceManager sourceFactory;

    @Inject
    @TensorData
    private JsonSerializer serializer;

    @Path( "/{id}" )
    @DELETE
    public Response delete( @PathParam( "id" ) final String id )
    {
        if ( !dataManager.deleteWorkspace( id ) )
        {
            return Response.noContent()
                           .build();
        }

        return Response.ok()
                       .build();
    }

    @Path( "/new" )
    @POST
    @Produces( MediaType.APPLICATION_JSON )
    public Response create( @Context final UriInfo uriInfo )
    {
        try
        {
            final GraphWorkspace workspace = dataManager.createWorkspace( new GraphWorkspaceConfiguration() );

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

    @Path( "/new/from" )
    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response createFrom( @Context final UriInfo uriInfo, @Context final HttpServletRequest request )
    {
        final GraphWorkspaceConfiguration config =
            ServletSerializerUtils.fromRequestBody( request, serializer, GraphWorkspaceConfiguration.class );
        try
        {
            final GraphWorkspace workspace = dataManager.createWorkspace( config );

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
                            @PathParam( "artifactId" ) final String artifactId,
                            @PathParam( "newVersion" ) final String newVersion,
                            @QueryParam( "for" ) final String oldVersion, @Context final UriInfo uriInfo )
    {
        Response response = Response.notModified()
                                    .build();

        GraphWorkspace ws;
        try
        {
            ws = dataManager.setCurrentWorkspace( id );
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

        final SingleVersion ver = VersionUtils.createSingleVersion( newVersion );

        boolean modified = false;

        final Set<ProjectVersionRef> oldRefs = new HashSet<ProjectVersionRef>();
        if ( oldVersion != null )
        {
            final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, oldVersion );
            oldRefs.add( ref );
            try
            {
                ws.selectVersion( ref, ver );
                modified = true;
            }
            catch ( final GraphDriverException e )
            {
                logger.error( "Failed to select version: %s (originally: %s) for GAV: %s. Reason: %s", e, ver,
                              newVersion, ref, e.getMessage() );
                return Response.serverError()
                               .build();
            }
        }
        else
        {
            try
            {
                final Set<ProjectVersionRef> matches =
                    dataManager.getMatchingGAVs( new ProjectRef( groupId, artifactId ) );
                for ( final ProjectVersionRef gav : matches )
                {
                    try
                    {
                        ws.selectVersion( gav, ver );
                        modified = true;
                    }
                    catch ( final GraphDriverException e )
                    {
                        logger.error( "Failed to select version: %s (originally: %s) for GAV: %s. Reason: %s", e, ver,
                                      newVersion, gav, e.getMessage() );
                        return Response.serverError()
                                       .build();
                    }
                }
            }
            catch ( final CartoDataException e )
            {
                logger.error( "Failed to retrieve matching GAVs for: %s:%s. Reason: %s", e, groupId, artifactId,
                              e.getMessage() );
                return Response.serverError()
                               .build();
            }
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
        ws.close();

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
            ws = dataManager.setCurrentWorkspace( id );
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
        ws.close();

        return response;
    }

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response list()
    {
        Response response = Response.notModified()
                                    .build();

        final Set<GraphWorkspace> ws = dataManager.getAllWorkspaces();

        if ( ws == null || ws.isEmpty() )
        {
            logger.debug( "No workspaces found." );
            return Response.status( Status.NOT_FOUND )
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
    public Response addSource( @PathParam( "id" ) final String id, @PathParam( "source" ) final String source,
                               @Context final UriInfo uriInfo )
    {
        Response response = Response.notModified()
                                    .build();

        GraphWorkspace ws;
        try
        {
            ws = dataManager.setCurrentWorkspace( id );
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

        boolean modified = false;

        if ( source != null )
        {
            final URI src = sourceFactory.createSourceURI( source );
            ws.addActiveSource( src );
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
        ws.close();

        return response;
    }

    @Path( "/{id}/pomloc/{pom-location}" )
    @PUT
    @Produces( MediaType.APPLICATION_JSON )
    public Response addPomLocation( @PathParam( "id" ) final String id,
                                    @PathParam( "pom-location" ) final String pomLoc, @Context final UriInfo uriInfo )
    {
        Response response = Response.notModified()
                                    .build();

        GraphWorkspace ws;
        try
        {
            ws = dataManager.setCurrentWorkspace( id );
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

        boolean modified = false;

        if ( pomLoc != null && !POM_ROOT_URI.equals( pomLoc ) )
        {
            final URI pomLocation = profileLocation( pomLoc );
            ws.addActivePomLocation( pomLocation );
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
        ws.close();

        return response;
    }
}
