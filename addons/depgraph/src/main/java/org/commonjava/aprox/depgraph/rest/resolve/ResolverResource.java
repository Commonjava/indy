package org.commonjava.aprox.depgraph.rest.resolve;

import static org.commonjava.aprox.depgraph.util.RequestUtils.getBooleanParamWithDefault;
import static org.commonjava.aprox.depgraph.util.RequestUtils.getLongParamWithDefault;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

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

import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.agg.AggregationOptions;
import org.commonjava.maven.cartographer.agg.DefaultAggregatorOptions;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/depgraph/resolve/{from: (.+)}" )
@Produces( MediaType.APPLICATION_JSON )
@RequestScoped
public class ResolverResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ResolveOps ops;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    @Inject
    private DiscoverySourceManager sourceManager;

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

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        final AggregationOptions options = createAggregationOptions( request, source );

        List<ProjectVersionRef> resolved;
        try
        {
            resolved = ops.resolve( from, options, ref );

            final String json = serializer.toString( Collections.singletonMap( "resolvedTopLevelGAVs", resolved ) );
            response = Response.ok( json )
                               .build();

        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to resolve graph: %s from: %s. Reason: %s", e, ref, from, e.getMessage() );

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

        final DefaultAggregatorOptions options = createAggregationOptions( request, source );
        options.setProcessIncompleteSubgraphs( true );

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

        try
        {
            final List<ProjectVersionRef> failed = ops.resolve( from, options, ref );

            final String json = serializer.toString( Collections.singletonMap( "failures", failed ) );

            response = Response.ok( json )
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

    private DefaultAggregatorOptions createAggregationOptions( final HttpServletRequest request, final URI source )
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
