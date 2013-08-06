package org.commonjava.aprox.depgraph.rest.resolve;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
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
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.aprox.depgraph.util.VariableTargetFilter;
import org.commonjava.maven.atlas.common.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.effective.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.effective.rel.ProjectRelationship;
import org.commonjava.tensor.data.CartoDataException;
import org.commonjava.tensor.data.CartoDataManager;
import org.commonjava.tensor.discover.DefaultDiscoveryConfig;
import org.commonjava.tensor.discover.DiscoveryResult;
import org.commonjava.tensor.discover.DiscoverySourceManager;
import org.commonjava.tensor.discover.ProjectRelationshipDiscoverer;
import org.commonjava.tensor.inject.TensorData;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.model.Listing;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/depgraph/project/resolve/{from: (.+)}" )
@Produces( MediaType.APPLICATION_JSON )
@RequestScoped
public class ProjectResolverResource
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
    private RequestAdvisor requestAdvisor;

    @Path( "/{g}/{a}/{v}" )
    @GET
    public Response resolve( @PathParam( "from" ) final String from, @PathParam( "g" ) final String groupId,
                             @PathParam( "a" ) final String artifactId, @PathParam( "v" ) final String version )
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

    @Path( "/{g}/{a}/{v}/variable" )
    @GET
    public Response resolveVariable( @PathParam( "from" ) final String from, @PathParam( "g" ) final String groupId,
                                     @PathParam( "a" ) final String artifactId, @PathParam( "v" ) final String version,
                                     @Context final HttpServletRequest request )
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

        final DefaultDiscoveryConfig config = new DefaultDiscoveryConfig( source );
        final ProjectRelationshipFilter filter =
            new VariableTargetFilter( requestAdvisor.createRelationshipFilter( request ) );

        ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            ref = discoverer.resolveSpecificVersion( ref, config );

            final Set<ProjectRelationship<?>> rels = data.getAllDirectRelationshipsWithExactSource( ref, filter );
            if ( rels != null && !rels.isEmpty() )
            {
                final String json = serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
                response = Response.ok( json )
                                   .build();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup incomplete subgraphs for: %s. Reason: %s", e, ref, e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/{g}/{a}/{v}/missing" )
    @GET
    public Response resolveMissing( @PathParam( "from" ) final String from, @PathParam( "g" ) final String groupId,
                                    @PathParam( "a" ) final String artifactId, @PathParam( "v" ) final String version,
                                    @Context final HttpServletRequest request )
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
            return Response.status( Status.BAD_REQUEST )
                           .entity( message )
                           .build();
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
        final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( request );

        ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            ref = discoverer.resolveSpecificVersion( ref, config );

            final Set<ProjectRelationship<?>> rels = data.getAllDirectRelationshipsWithExactSource( ref, filter );
            if ( rels != null )
            {
                for ( final Iterator<ProjectRelationship<?>> it = rels.iterator(); it.hasNext(); )
                {
                    final ProjectRelationship<?> rel = it.next();
                    if ( data.contains( rel.getTarget()
                                           .asProjectVersionRef() ) )
                    {
                        it.remove();
                    }
                }

                if ( !rels.isEmpty() )
                {
                    final String json = serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
                    response = Response.ok( json )
                                       .build();
                }
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup incomplete subgraphs for: %s. Reason: %s", e, ref, e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

}
