package org.commonjava.aprox.depgraph.rest.render;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.io.IOException;
import java.io.StringWriter;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.commonjava.aprox.depgraph.util.AggregatorConfigUtils;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.agg.AggregatorConfig;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.GraphRenderingOps;
import org.commonjava.util.logging.Logger;

@Path( "/depgraph/render/graph" )
@RequestScoped
public class GraphRenderingResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private GraphRenderingOps ops;

    @Inject
    private RequestAdvisor requestAdvisor;

    @Path( "/bom/{g}/{a}/{v}" )
    @POST
    public Response bomFor( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                            @PathParam( "v" ) final String version, @Context final HttpServletRequest request )
    {
        Response response;

        AggregatorConfig config = null;
        try
        {
            config = AggregatorConfigUtils.read( request.getInputStream() );
            final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( request );

            final Model model = ops.generateBOM( new ProjectVersionRef( groupId, artifactId, version ), filter, config.getRoots() );

            final StringWriter writer = new StringWriter();
            new MavenXpp3Writer().write( writer, model );

            final String out = writer.toString();
            response = Response.ok( out )
                               .build();

        }
        catch ( final IOException e )
        {
            logger.error( "Failed to read list of GAVs from POST body: %s", e, e.getMessage() );
            response = Response.serverError()
                               .build();
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to retrieve web for: %s. Reason: %s", e, config, e.getMessage() );
            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/dotfile/{g}/{a}/{v}" )
    @Produces( "text/x-graphviz" )
    @GET
    public Response dotfile( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                             @PathParam( "v" ) final String version, @Context final HttpServletRequest request )
    {
        Response response;

        //        final DiscoveryConfig discovery = createDiscoveryConfig( request, null, sourceFactory );
        final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( request );
        try
        {
            final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
            final String dotfile = ops.dotfile( ref, filter, ref );

            if ( dotfile != null )
            {
                response = Response.ok( dotfile )
                                   .build();
            }
            else
            {
                logger.error( "Cannot find graph: %s:%s:%s", groupId, artifactId, version );
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }

        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup project graph for: %s:%s:%s. Reason: %s", e, groupId, artifactId, version, e.getMessage() );

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

    @Path( "/tree/{g}/{a}/{v}" )
    @Produces( MediaType.TEXT_PLAIN )
    @GET
    public Response depTree( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                             @PathParam( "v" ) final String version, @Context final HttpServletRequest request,
                             @QueryParam( "s" ) @DefaultValue( "runtime" ) final String scope,
                             @QueryParam( "c-t" ) @DefaultValue( "true" ) final boolean collapseTransitives )
    {
        Response response = Response.status( NOT_FOUND )
                                    .build();
        try
        {

            final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

            final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( request );

            final String tree =
                ops.depTree( ref, filter, scope == null ? DependencyScope.runtime : DependencyScope.getScope( scope ), collapseTransitives );

            if ( tree != null )
            {
                response = Response.ok( tree )
                                   .build();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup project graph for: %s:%s:%s. Reason: %s", e, groupId, artifactId, version, e.getMessage() );

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
