package org.commonjava.aprox.depgraph.rest;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;

import java.util.ArrayList;
import java.util.Collections;
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

import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.maven.atlas.common.DependencyScope;
import org.commonjava.maven.atlas.common.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.common.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.effective.filter.DependencyOnlyFilter;
import org.commonjava.maven.atlas.effective.filter.ExtensionOnlyFilter;
import org.commonjava.maven.atlas.effective.filter.OrFilter;
import org.commonjava.maven.atlas.effective.filter.PluginOnlyFilter;
import org.commonjava.maven.atlas.effective.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.effective.rel.ProjectRelationship;
import org.commonjava.tensor.data.CartoDataException;
import org.commonjava.tensor.data.CartoDataManager;
import org.commonjava.tensor.event.TensorEventFunnel;
import org.commonjava.tensor.inject.TensorData;
import org.commonjava.tensor.util.ProjectVersionRefComparator;
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
    private CartoDataManager data;

    //    @Inject
    //    private ProjectRelationshipDiscoverer discoverer;

    @Inject
    private TensorEventFunnel funnel;

    @Inject
    @TensorData
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
            Set<String> errors = data.getErrors( ref );
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
            final Set<ProjectVersionRef> refs = data.getAllStoredProjectRefs();
            final List<ProjectVersionRef> list = new ArrayList<ProjectVersionRef>();
            if ( refs != null )
            {
                if ( groupIdPattern != null || artifactIdPattern != null )
                {
                    final String gip = groupIdPattern == null ? ".*" : groupIdPattern.replaceAll( "\\*", ".*" );
                    final String aip = artifactIdPattern == null ? ".*" : artifactIdPattern.replaceAll( "\\*", ".*" );

                    logger.info( "Filtering %d projects using groupId pattern: '%s' and artifactId pattern: '%s'",
                                 refs.size(), gip, aip );

                    for ( final ProjectVersionRef ref : refs )
                    {
                        if ( ref.getGroupId()
                                .matches( gip ) && ref.getArtifactId()
                                                      .matches( aip ) )
                        {
                            list.add( ref );
                        }
                    }
                }
                else
                {
                    logger.info( "Returning all %d projects", refs.size() );
                    list.addAll( refs );
                }

            }

            if ( !list.isEmpty() )
            {
                Collections.sort( list, new ProjectVersionRefComparator() );

                final String json = serializer.toString( new Listing<ProjectVersionRef>( list ) );
                response = Response.ok( json )
                                   .build();
            }
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
            final ProjectVersionRef parent = data.getParent( new ProjectVersionRef( groupId, artifactId, version ) );
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
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Invalid version in request: '%s'. Reason: %s", e, version, e.getMessage() );

            response = Response.status( BAD_REQUEST )
                               .entity( "Invalid version: '" + version + "'" )
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
        final Set<DependencyOnlyFilter> subs = new HashSet<DependencyOnlyFilter>();
        if ( scopesStr != null && scopesStr.trim()
                                           .length() > 0 )
        {
            final String[] scopes = scopesStr.split( "\\s*,\\s*" );
            for ( final String scope : scopes )
            {
                final DependencyScope s = DependencyScope.getScope( scope );
                if ( s != null )
                {
                    subs.add( new DependencyOnlyFilter( s, false, true, false ) );
                }
            }
        }

        if ( subs.isEmpty() )
        {
            subs.add( new DependencyOnlyFilter( DependencyScope.test, false, true, true ) );
        }

        Response response = Response.status( NO_CONTENT )
                                    .build();

        try
        {
            final Set<ProjectRelationship<?>> deps =
                data.getAllDirectRelationshipsWithExactSource( new ProjectVersionRef( groupId, artifactId, version ),
                                                               new OrFilter( subs ) );

            if ( deps != null )
            {
                final String json = serializer.toString( new Listing<ProjectRelationship<?>>( deps ) );
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
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Invalid version in request: '%s'. Reason: %s", e, version, e.getMessage() );

            response = Response.status( BAD_REQUEST )
                               .entity( "Invalid version: '" + version + "'" )
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

        try
        {
            final Set<ProjectRelationship<?>> deps =
                data.getAllDirectRelationshipsWithExactSource( new ProjectVersionRef( groupId, artifactId, version ),
                                                               new PluginOnlyFilter() );

            if ( deps != null )
            {
                final String json = serializer.toString( new Listing<ProjectRelationship<?>>( deps ) );
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
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Invalid version in request: '%s'. Reason: %s", e, version, e.getMessage() );

            response = Response.status( BAD_REQUEST )
                               .entity( "Invalid version: '" + version + "'" )
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

        try
        {
            final Set<ProjectRelationship<?>> deps =
                data.getAllDirectRelationshipsWithExactSource( new ProjectVersionRef( groupId, artifactId, version ),
                                                               new ExtensionOnlyFilter() );

            if ( deps != null )
            {
                final String json = serializer.toString( new Listing<ProjectRelationship<?>>( deps ) );
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
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Invalid version in request: '%s'. Reason: %s", e, version, e.getMessage() );

            response = Response.status( BAD_REQUEST )
                               .entity( "Invalid version: '" + version + "'" )
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

        try
        {
            final Set<ProjectRelationship<?>> rels =
                data.getAllDirectRelationshipsWithExactSource( new ProjectVersionRef( groupId, artifactId, version ),
                                                               filter );

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
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Invalid version in request: '%s'. Reason: %s", e, version, e.getMessage() );

            response = Response.status( BAD_REQUEST )
                               .entity( "Invalid version: '" + version + "'" )
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

        try
        {
            final Set<ProjectRelationship<?>> rels =
                data.getAllDirectRelationshipsWithExactTarget( new ProjectVersionRef( groupId, artifactId, version ),
                                                               filter );

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
