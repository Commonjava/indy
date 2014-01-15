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

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.commonjava.aprox.depgraph.rest.jaxrs.util.RequestUtils.parseGAV;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.depgraph.rest.jaxrs.util.RequestAdvisor;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.GraphOps;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.model.Listing;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/depgraph/rel" )
@Produces( MediaType.APPLICATION_JSON )
@RequestScoped
public class GraphResource
{
    private final Logger logger = new Logger( getClass() );

    @Inject
    private GraphOps ops;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    @Inject
    private RequestAdvisor requestAdvisor;

    @Path( "/reindex{gav: ([^/]+/[^/]+/[^/]+)?}" )
    @GET
    public Response reindex( @PathParam( "gav" ) final String gav, @Context final HttpServletRequest request )
    {
        Response response = Response.ok()
                                    .build();

        ProjectVersionRef ref = null;
        try
        {
            if ( gav != null )
            {
                ref = parseGAV( gav );
            }

            if ( ref != null )
            {
                ops.reindex( ref );
            }
            else
            {
                ops.reindexAll();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to reindex: %s. Reason: %s", e, ref == null ? "all projects" : ref, e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/errors{gav: ([^/]+/[^/]+/[^/]+)?}" )
    @GET
    public Response errors( @PathParam( "gav" ) final String gav, @Context final HttpServletRequest request )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();

        ProjectVersionRef ref = null;
        try
        {
            if ( gav != null )
            {
                ref = parseGAV( gav );
            }

            Map<ProjectVersionRef, Set<String>> errors;
            if ( ref != null )
            {
                logger.info( "Retrieving project errors in graph: %s", ref );
                errors = ops.getErrors( ref );
            }
            else
            {
                logger.info( "Retrieving ALL project errors" );
                errors = ops.getAllErrors();
            }

            if ( errors != null )
            {
                final String json = serializer.toString( errors );
                logger.info( "Resulting JSON:\n\n%s\n\n", json );
                response = Response.ok( json )
                                   .build();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup resolution errors for: %s. Reason: %s", e, ref == null ? "all projects"
                            : ref, e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/incomplete{gav: ([^/]+/[^/]+/[^/]+)?}" )
    @GET
    public Response incomplete( @PathParam( "gav" ) final String gav, @Context final HttpServletRequest request )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();

        final ProjectVersionRef ref = gav == null ? null : parseGAV( gav );
        try
        {
            final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( request );

            final Set<ProjectVersionRef> result =
                ref == null ? ops.getAllIncomplete( filter ) : ops.getIncomplete( ref, filter );

            if ( result != null )
            {
                final String json = serializer.toString( new Listing<ProjectVersionRef>( result ) );
                response = Response.ok( json )
                                   .build();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup incomplete subgraphs for: %s. Reason: %s", e, ref == null ? "all projects"
                            : ref, e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/variable{gav: (.+/.+/.+)?}" )
    @GET
    public Response variable( @PathParam( "gav" ) final String gav, @Context final HttpServletRequest request )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();

        final ProjectVersionRef ref = gav == null ? null : parseGAV( gav );
        try
        {
            final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( request );

            final Set<ProjectVersionRef> result =
                ref == null ? ops.getAllVariable( filter ) : ops.getVariable( ref, filter );

            if ( result != null )
            {
                final String json = serializer.toString( new Listing<ProjectVersionRef>( result ) );
                response = Response.ok( json )
                                   .build();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup variable subgraphs for: %s. Reason: %s", e, ref == null ? "all projects"
                            : ref, e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/ancestry/{g}/{a}/{v}" )
    @GET
    public Response ancestryOf( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                                @PathParam( "v" ) final String version )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();
        try
        {
            final List<ProjectVersionRef> ancestry =
                ops.getAncestry( new ProjectVersionRef( groupId, artifactId, version ) );

            if ( ancestry != null )
            {
                final String json = serializer.toString( new Listing<ProjectVersionRef>( ancestry ) );
                response = Response.ok( json )
                                   .build();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup ancestry for: %s:%s:%s. Reason: %s", e, groupId, artifactId, version,
                          e.getMessage() );

            response = Response.serverError()
                               .build();
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Invalid version in request: '%s'. Reason: %s", e, version, e.getMessage() );
            response = Response.status( BAD_REQUEST )
                               .entity( "Invalid version: '" + version + "'" )
                               .build();
        }

        return response;
    }

    @Path( "/build-order/{g}/{a}/{v}" )
    @GET
    public Response buildOrder( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                                @PathParam( "v" ) final String version, @Context final HttpServletRequest request )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();

        //        final DiscoveryConfig discovery = createDiscoveryConfig( request, null, sourceFactory );
        final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( request );

        try
        {
            final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

            final BuildOrder buildOrder = ops.getBuildOrder( ref, filter );

            final String json = serializer.toString( buildOrder );

            response = Response.ok( json )
                               .build();
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup project graph for: %s:%s:%s. Reason: %s", e, groupId, artifactId, version,
                          e.getMessage() );

            response = Response.serverError()
                               .build();
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Invalid version in request: '%s'. Reason: %s", e, version, e.getMessage() );
            response = Response.status( BAD_REQUEST )
                               .entity( "Invalid version: '" + version + "'" )
                               .build();
        }

        return response;
    }

    @Path( "/project/{g}/{a}/{v}" )
    @GET
    public Response projectGraph( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                                  @PathParam( "v" ) final String version, @Context final HttpServletRequest request )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();
        try
        {
            //            final DiscoveryConfig discovery = createDiscoveryConfig( request, null, sourceFactory );
            final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( request );

            final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
            final EProjectGraph graph = ops.getProjectGraph( filter, ref );

            if ( graph != null )
            {
                final ResponseBuilder rb = Response.ok();

                requestAdvisor.checkForIncompleteOrVariableGraphs( graph, rb );

                final String json = serializer.toString( graph );
                response = rb.entity( json )
                             .build();
            }
            else
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }

        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup project graph for: %s:%s:%s. Reason: %s", e, groupId, artifactId, version,
                          e.getMessage() );

            response = Response.serverError()
                               .build();
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Invalid version in request: '%s'. Reason: %s", e, version, e.getMessage() );
            response = Response.status( BAD_REQUEST )
                               .entity( "Invalid version: '" + version + "'" )
                               .build();
        }

        return response;
    }

}
