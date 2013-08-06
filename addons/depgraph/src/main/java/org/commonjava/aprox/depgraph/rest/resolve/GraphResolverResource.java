package org.commonjava.aprox.depgraph.rest.resolve;

import static org.commonjava.aprox.depgraph.util.RequestUtils.getBooleanParamWithDefault;
import static org.commonjava.aprox.depgraph.util.RequestUtils.getLongParamWithDefault;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.depgraph.preset.WorkspaceRecorder;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.maven.atlas.common.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.effective.EProjectGraph;
import org.commonjava.maven.atlas.effective.filter.ProjectRelationshipFilter;
import org.commonjava.tensor.agg.AggregationOptions;
import org.commonjava.tensor.agg.DefaultAggregatorOptions;
import org.commonjava.tensor.agg.GraphAggregator;
import org.commonjava.tensor.data.CartoDataException;
import org.commonjava.tensor.data.CartoDataManager;
import org.commonjava.tensor.discover.DefaultDiscoveryConfig;
import org.commonjava.tensor.discover.DiscoveryResult;
import org.commonjava.tensor.discover.DiscoverySourceManager;
import org.commonjava.tensor.discover.ProjectRelationshipDiscoverer;
import org.commonjava.tensor.inject.TensorData;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/depgraph/graph/resolve/{from: (.+)}" )
@Produces( MediaType.APPLICATION_JSON )
@RequestScoped
public class GraphResolverResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private CartoDataManager data;

    @Inject
    private ProjectRelationshipDiscoverer discoverer;

    @Inject
    @TensorData
    private JsonSerializer serializer;

    @Inject
    private DiscoverySourceManager sourceManager;

    @Inject
    private GraphAggregator aggregator;

    @Inject
    private RequestAdvisor requestAdvisor;

    @Path( "/{g}/{a}/{v}" )
    @GET
    public Response resolveGraph( @PathParam( "from" ) final String from, @PathParam( "g" ) final String groupId,
                                  @PathParam( "a" ) final String artifactId, @PathParam( "v" ) final String version,
                                  @Context final HttpServletRequest request,
                                  @QueryParam( "recurse" ) @DefaultValue( "true" ) final boolean recurse )
    {
        Response response = Response.status( Status.NO_CONTENT )
                                    .build();

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

        final URI source = sourceManager.createSourceURI( from );
        if ( source == null )
        {
            final String message =
                String.format( "Invalid source format: '%s'. Use the form: '%s' instead.", from,
                               sourceManager.getFormatHint() );
            logger.warn( message );
            response = Response.status( Status.BAD_REQUEST )
                               .entity( message )
                               .build();
            return response;
        }

        try
        {
            sourceManager.activateWorkspaceSources( from );
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to activate source locations for source: %s. Reason: %s", e, from, e.getMessage() );
            return Response.serverError()
                           .build();
        }

        final DefaultDiscoveryConfig config = new DefaultDiscoveryConfig( source );
        try
        {
            final DiscoveryResult result = discoverer.discoverRelationships( ref, config );
            if ( result != null && data.contains( result.getSelectedRef() ) )
            {
                final ProjectVersionRef selected = result.getSelectedRef();

                final AggregationOptions options = createAggregationOptions( request, source );
                final ProjectRelationshipFilter filter = options.getFilter();

                final EProjectGraph graph = data.getProjectGraph( filter, selected );
                if ( recurse )
                {
                    aggregator.connectIncomplete( graph, options );
                }

                if ( filter instanceof WorkspaceRecorder )
                {
                    ( (WorkspaceRecorder) filter ).save( data.getCurrentWorkspace() );
                }

                final String json = serializer.toString( Collections.singletonMap( "resolvedGAV", selected ) );
                response = Response.ok( json )
                                   .build();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to discover: %s. Reason: %s", e, ref, e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/{g}/{a}/{v}/incomplete" )
    @GET
    public Response resolveIncomplete( @PathParam( "from" ) final String from, @PathParam( "g" ) final String groupId,
                                       @PathParam( "a" ) final String artifactId,
                                       @PathParam( "v" ) final String version,
                                       @QueryParam( "recurse" ) @DefaultValue( "false" ) final boolean recurse,
                                       @Context final HttpServletRequest request,
                                       @Context final HttpServletResponse resp )
        throws IOException
    {
        Response response = Response.status( Status.NO_CONTENT )
                                    .build();

        final URI source = sourceManager.createSourceURI( from );
        if ( source == null )
        {
            final String message =
                String.format( "Invalid source format: '%s'. Use the form: '%s' instead.", from,
                               sourceManager.getFormatHint() );
            logger.warn( message );
            response = Response.status( Status.BAD_REQUEST )
                               .entity( message )
                               .build();
            return response;
        }

        try
        {
            sourceManager.activateWorkspaceSources( from );
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to activate source locations for source: %s. Reason: %s", e, from, e.getMessage() );
            return Response.serverError()
                           .build();
        }

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

        try
        {
            final Set<ProjectVersionRef> seen = new HashSet<ProjectVersionRef>();

            final PrintStream ps = new PrintStream( resp.getOutputStream() );

            final AggregationOptions options = createAggregationOptions( request, source );
            int changed;
            do
            {
                final Set<ProjectVersionRef> incomplete = requestAdvisor.getIncomplete( ref, request );

                changed = 0;
                if ( incomplete != null && !incomplete.isEmpty() )
                {
                    for ( final ProjectVersionRef r : incomplete )
                    {
                        if ( seen.contains( r ) )
                        {
                            continue;
                        }

                        changed++;
                        try
                        {
                            final DiscoveryResult result =
                                discoverer.discoverRelationships( r, options.getDiscoveryConfig() );

                            if ( result != null )
                            {
                                ps.println( result.getSelectedRef() );
                            }
                        }
                        catch ( final CartoDataException e )
                        {
                            ps.printf( "%s: ERROR %s\n", r, e.getMessage() );
                        }

                        seen.add( r );
                    }
                }
            }
            while ( recurse && changed > 0 );

            response = Response.ok()
                               .build();
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

    private AggregationOptions createAggregationOptions( final HttpServletRequest request, final URI source )
    {
        final DefaultAggregatorOptions options = new DefaultAggregatorOptions();
        options.setFilter( requestAdvisor.createRelationshipFilter( request ) );

        final DefaultDiscoveryConfig dconf = new DefaultDiscoveryConfig( source );
        dconf.setEnabled( true );
        dconf.setTimeoutMillis( getLongParamWithDefault( request, "timeout", dconf.getTimeoutMillis() ) );

        options.setDiscoveryConfig( dconf );

        options.setProcessIncompleteSubgraphs( getBooleanParamWithDefault( request, "incomplete", true ) );
        options.setProcessVariableSubgraphs( getBooleanParamWithDefault( request, "variable", true ) );

        logger.info( "AGGREGATOR OPTIONS:\n\n%s\n\n", options );

        return options;
    }

}
