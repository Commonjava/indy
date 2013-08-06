package org.commonjava.aprox.depgraph.rest;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.commonjava.aprox.depgraph.util.RequestUtils.parseGAV;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.maven.atlas.common.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.common.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.effective.EProjectGraph;
import org.commonjava.maven.atlas.effective.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.effective.traverse.BuildOrderTraversal;
import org.commonjava.maven.atlas.effective.traverse.FilteringTraversal;
import org.commonjava.maven.atlas.effective.traverse.ProjectNetTraversal;
import org.commonjava.maven.atlas.effective.traverse.TransitiveDependencyTraversal;
import org.commonjava.maven.atlas.effective.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.effective.traverse.print.DependencyTreeRelationshipPrinter;
import org.commonjava.maven.atlas.effective.traverse.print.StructurePrintingTraversal;
import org.commonjava.maven.atlas.spi.GraphDriverException;
import org.commonjava.tensor.data.CartoDataException;
import org.commonjava.tensor.data.CartoDataManager;
import org.commonjava.tensor.discover.DiscoverySourceManager;
import org.commonjava.tensor.event.TensorEventFunnel;
import org.commonjava.tensor.inject.TensorData;
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
    private CartoDataManager data;

    //    @Inject
    //    private ProjectRelationshipDiscoverer discoverer;

    @Inject
    private TensorEventFunnel funnel;

    @Inject
    @TensorData
    private JsonSerializer serializer;

    @Inject
    private DiscoverySourceManager sourceFactory;

    @Inject
    private RequestAdvisor requestAdvisor;

    @Path( "/reindex{gav: (.+)?}" )
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
                data.reindex( ref );
            }
            else
            {
                data.reindexAll();
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

    @Path( "/errors{gav: (.+)?}" )
    @GET
    public Response errors( @PathParam( "gav" ) final String gav, @Context final HttpServletRequest request )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();

        ProjectVersionRef ref = null;
        try
        {
            Map<ProjectVersionRef, Set<String>> errors;
            if ( gav != null )
            {
                ref = parseGAV( gav );
            }

            if ( ref != null )
            {
                logger.info( "Retrieving project errors in graph: %s", ref );
                errors = data.getProjectErrorsInGraph( ref );
            }
            else
            {
                logger.info( "Retrieving ALL project errors" );
                errors = data.getAllProjectErrors();
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
            logger.error( "Failed to lookup incomplete subgraphs for: %s. Reason: %s", e, ref == null ? "all projects"
                            : ref, e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/incomplete{gav: (.+)?}" )
    @GET
    public Response incomplete( @PathParam( "gav" ) final String gav, @Context final HttpServletRequest request )
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

            final Set<ProjectVersionRef> incomplete = requestAdvisor.getIncomplete( ref, request );

            if ( incomplete != null )
            {
                final String json = serializer.toString( new Listing<ProjectVersionRef>( incomplete ) );
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

    @Path( "/variable{gav: (.+)?}" )
    @GET
    public Response variable( @PathParam( "gav" ) final String gav, @Context final HttpServletRequest request )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();

        ProjectVersionRef ref = null;
        try
        {
            Set<ProjectVersionRef> variable;
            if ( gav != null )
            {
                ref = parseGAV( gav );
            }

            if ( ref != null )
            {
                variable = data.getVariableSubgraphsFor( ref );

                if ( variable != null )
                {
                    final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( request );

                    if ( filter != null )
                    {
                        // TODO: This is non-optimal...if the ref == null, we can't do a path filter.
                        variable = data.pathFilter( filter, variable, ref );
                    }
                }
            }
            else
            {
                variable = data.getAllVariableSubgraphs();
            }

            if ( variable != null )
            {
                final String json = serializer.toString( new Listing<ProjectVersionRef>( variable ) );
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
                data.getAncestry( new ProjectVersionRef( groupId, artifactId, version ) );

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
            //            ref = discoverer.resolveSpecificVersion( ref, discovery );

            final EProjectGraph graph = data.getProjectGraph( ref );

            //            if ( graph == null && options.isDiscoveryEnabled() )
            //            {
            //                logger.info( "Performing discovery for: %s", ref );
            //                discoverer.discoverRelationships( ref, options.getDiscoveryConfig() );
            //                funnel.waitForGraph( ref, options.getDiscoveryTimeoutMillis() );
            //
            //                graph = data.getProjectGraph( ref );
            //
            //                if ( graph == null )
            //                {
            //                    response = Response.status( Status.NOT_FOUND )
            //                                       .build();
            //                }
            //            }

            if ( graph != null )
            {
                final ResponseBuilder rb = Response.ok();
                requestAdvisor.checkForIncompleteOrVariableGraphs( graph, rb );
                //                logger.info( "Activating graph aggregator with options: %s", options );
                //                if ( options.isDiscoveryEnabled() )
                //                {
                //                    graph = aggregator.connectSubgraphs( graph, options );
                //                }

                final BuildOrderTraversal traversal = new BuildOrderTraversal( filter );

                logger.info( "Performing build-order traversal for graph: %s", ref );
                graph.traverse( traversal );

                final BuildOrder buildOrder = traversal.getBuildOrder();
                logger.info( "Got build-order with %d elements for graph: %s", buildOrder.getOrder()
                                                                                         .size(), ref );

                final String json = serializer.toString( buildOrder );

                response = Response.ok( json )
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
        catch ( final GraphDriverException e )
        {
            logger.error( "Failed to filter project graph for: %s:%s:%s. Reason: %s", e, groupId, artifactId, version,
                          e.getMessage() );

            response = Response.serverError()
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
            final EProjectGraph graph = data.getProjectGraph( filter, ref );

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

    @Path( "/tree/{g}/{a}/{v}" )
    @Produces( MediaType.TEXT_PLAIN )
    @GET
    public Response depTree( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                             @PathParam( "v" ) final String version, @Context final HttpServletRequest request,
                             @QueryParam( "s" ) @DefaultValue( "runtime" ) final String scope,
                             @QueryParam( "c-t" ) @DefaultValue( "true" ) final boolean collapseTransitives )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();
        try
        {

            final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

            //            final DiscoveryConfig discovery = createDiscoveryConfig( request, null, sourceFactory );
            final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( request );

            //            ref = discoverer.resolveSpecificVersion( ref, discovery );

            final EProjectGraph graph = data.getProjectGraph( ref );
            //            if ( graph == null && options.isDiscoveryEnabled() )
            //            {
            //                discoverer.discoverRelationships( ref, options.getDiscoveryConfig() );
            //                funnel.waitForGraph( ref, options.getDiscoveryTimeoutMillis() );
            //
            //                graph = data.getProjectGraph( ref );
            //
            //                if ( graph == null )
            //                {
            //                    response = Response.status( Status.NOT_FOUND )
            //                                       .build();
            //                }
            //            }

            if ( graph != null )
            {
                final ResponseBuilder rb = Response.ok();
                requestAdvisor.checkForIncompleteOrVariableGraphs( graph, rb );

                //                if ( options.isDiscoveryEnabled() )
                //                {
                //                    graph = aggregator.connectSubgraphs( graph, options );
                //                }

                //                if ( collapseTransitives )
                //                {
                //                    final TransitiveDependencyTransformer trans =
                //                        new TransitiveDependencyTransformer( DependencyScope.getScope( scope ) );
                //                    graph.traverse( trans );
                //
                //                    graph = (EProjectGraph) trans.getTransformedNetwork();
                //                }
                //

                ProjectNetTraversal t;
                if ( collapseTransitives )
                {
                    t = new TransitiveDependencyTraversal( filter );
                }
                else
                {
                    t = new FilteringTraversal( filter );
                }

                final StructurePrintingTraversal printer =
                    new StructurePrintingTraversal( t, new DependencyTreeRelationshipPrinter() );

                graph.traverse( printer );

                final String structure = printer.printStructure( ref );
                //                logger.info( "Got dep-tree for %s:\n\n%s", ref, structure );

                response = rb.entity( structure )
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
        catch ( final GraphDriverException e )
        {
            logger.error( "Failed to filter project graph for: %s:%s:%s. Reason: %s", e, groupId, artifactId, version,
                          e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

}
