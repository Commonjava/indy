package org.commonjava.aprox.depgraph.rest;

import static javax.ws.rs.core.Response.Status.NO_CONTENT;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.maven.atlas.graph.filter.DependencyOnlyFilter;
import org.commonjava.maven.atlas.graph.filter.ExtensionOnlyFilter;
import org.commonjava.maven.atlas.graph.filter.PluginOnlyFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.GraphOps;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.model.Listing;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/depgraph/project" )
@Produces( MediaType.APPLICATION_JSON )
@RequestScoped
public class ProjectResource
{
    private final Logger logger = new Logger( getClass() );

    @Inject
    private GraphOps ops;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    @Inject
    private RequestAdvisor requestAdvisor;

    @Path( "/{g}/{a}/{v}/errors" )
    @GET
    public Response errors( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                            @PathParam( "v" ) final String version )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            Set<String> errors = ops.getProjectErrors( ref );
            if ( errors == null )
            {
                errors = new HashSet<String>();
            }

            final String json = serializer.toString( new Listing<String>( errors ) );
            response = Response.ok( json )
                               .build();
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup errors for: %s. Reason: %s", e, ref == null ? "all projects" : ref,
                          e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/list" )
    @GET
    public Response list( @QueryParam( "g" ) final String groupIdPattern,
                          @QueryParam( "a" ) final String artifactIdPattern )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();

        try
        {
            final List<ProjectVersionRef> matching = ops.listProjects( groupIdPattern, artifactIdPattern );
            final String json = serializer.toString( new Listing<ProjectVersionRef>( matching ) );
            response = Response.ok( json )
                               .build();
        }
        catch ( final CartoDataException e )
        {
            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/{g}/{a}/{v}/parent" )
    @GET
    public Response parentOf( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                              @PathParam( "v" ) final String version )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();

        try
        {
            final ProjectVersionRef parent =
                ops.getProjectParent( new ProjectVersionRef( groupId, artifactId, version ) );
            if ( parent != null )
            {
                final String json = serializer.toString( parent );
                response = Response.ok( json )
                                   .build();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup parent for: %s:%s:%s. Reason: %s", e, groupId, artifactId, version,
                          e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/{g}/{a}/{v}/dependencies" )
    @GET
    public Response dependenciesOf( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                                    @PathParam( "v" ) final String version,
                                    @QueryParam( "scopes" ) final String scopesStr )
    {
        DependencyOnlyFilter filter;
        if ( scopesStr != null && scopesStr.trim()
                                           .length() > 0 )
        {
            final String[] scopes = scopesStr.split( "\\s*,\\s*" );
            final Set<DependencyScope> ds = new HashSet<>( scopes.length );
            for ( final String scope : scopes )
            {
                final DependencyScope s = DependencyScope.getScope( scope );
                if ( s != null )
                {
                    ds.add( s );
                }
            }

            filter = new DependencyOnlyFilter( false, true, true, ds.toArray( new DependencyScope[ds.size()] ) );
        }
        else
        {
            filter = new DependencyOnlyFilter();
        }

        Response response = Response.status( NO_CONTENT )
                                    .build();

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            final Set<ProjectRelationship<?>> rels = ops.getDirectRelationshipsFrom( ref, filter );

            if ( rels != null )
            {
                final String json = serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
                response = Response.ok( json )
                                   .build();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup dependencies for: %s:%s:%s. Reason: %s", e, groupId, artifactId, version,
                          e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/{g}/{a}/{v}/plugins" )
    @GET
    public Response pluginsOf( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                               @PathParam( "v" ) final String version )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            final Set<ProjectRelationship<?>> rels = ops.getDirectRelationshipsFrom( ref, new PluginOnlyFilter() );

            if ( rels != null )
            {
                final String json = serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
                response = Response.ok( json )
                                   .build();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup plugins for: %s:%s:%s. Reason: %s", e, groupId, artifactId, version,
                          e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/{g}/{a}/{v}/extensions" )
    @GET
    public Response extensionsOf( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                                  @PathParam( "v" ) final String version )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            final Set<ProjectRelationship<?>> rels = ops.getDirectRelationshipsFrom( ref, new ExtensionOnlyFilter() );

            if ( rels != null )
            {
                final String json = serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
                response = Response.ok( json )
                                   .build();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup extensions for: %s:%s:%s. Reason: %s", e, groupId, artifactId, version,
                          e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/{g}/{a}/{v}/relationships" )
    @GET
    public Response relationshipsSpecifiedBy( @PathParam( "g" ) final String groupId,
                                              @PathParam( "a" ) final String artifactId,
                                              @PathParam( "v" ) final String version,
                                              @Context final HttpServletRequest request )
    {
        final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( request );
        Response response = Response.status( NO_CONTENT )
                                    .build();

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            final Set<ProjectRelationship<?>> rels = ops.getDirectRelationshipsFrom( ref, filter );

            if ( rels != null )
            {
                final String json = serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
                response = Response.ok( json )
                                   .build();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup relationships specified by: %s:%s:%s. Reason: %s", e, groupId, artifactId,
                          version, e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/{g}/{a}/{v}/users" )
    @GET
    public Response relationshipsTargeting( @PathParam( "g" ) final String groupId,
                                            @PathParam( "a" ) final String artifactId,
                                            @PathParam( "v" ) final String version,
                                            @Context final HttpServletRequest request )
    {
        final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( request );
        Response response = Response.status( NO_CONTENT )
                                    .build();

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            final Set<ProjectRelationship<?>> rels = ops.getDirectRelationshipsFrom( ref, filter );

            if ( rels != null )
            {
                final String json = serializer.toString( new Listing<ProjectRelationship<?>>( rels ) );
                response = Response.ok( json )
                                   .build();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup relationships specified by: %s:%s:%s. Reason: %s", e, groupId, artifactId,
                          version, e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

}
