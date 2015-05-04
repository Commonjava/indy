/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.depgraph.jaxrs;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.model.util.HttpUtils.parseQueryMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.depgraph.rest.ProjectController;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/api/depgraph/project" )
@ApplicationScoped
public class ProjectResource
    implements AproxResources
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ProjectController controller;

    @Path( "/{groupId}/{artifactId}/{version}/errors" )
    @GET
    public Response errors( @PathParam( "groupId" ) final String gid, @PathParam( "artifactId" ) final String aid,
                            @PathParam( "version" ) final String ver, @QueryParam( "wsid" ) final String wsid )
    {
        Response response = null;
        String json = null;
        try
        {
            json = controller.errors( gid, aid, ver, wsid );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }

        if ( response == null )
        {
            if ( json != null )
            {
                response = formatOkResponseWithJsonEntity( json );
            }
            else
            {
                response = Response.status( Status.NO_CONTENT )
                                   .build();
            }
        }
        return response;
    }

    @Path( "/list" )
    @GET
    public Response list( final @QueryParam( "g" ) String groupIdPattern,
                          final @QueryParam( "a" ) String artifactIdPattern, final @QueryParam( "wsid" ) String wsid )
    {
        Response response = null;
        String json = null;
        try
        {
            json = controller.list( groupIdPattern, artifactIdPattern, wsid );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }

        if ( response == null )
        {
            if ( json != null )
            {
                response = formatOkResponseWithJsonEntity( json );
            }
            else
            {
                response = Response.status( Status.NO_CONTENT )
                                   .build();
            }
        }

        return response;
    }

    @Path( "/{groupId}/{artifactId}/{version}/parent" )
    @GET
    public Response parentOf( @PathParam( "groupId" ) final String gid, @PathParam( "artifactId" ) final String aid,
                              @PathParam( "version" ) final String ver, @QueryParam( "wsid" ) final String wsid )
    {
        Response response = null;
        String json = null;
        try
        {
            json = controller.parentOf( gid, aid, ver, wsid );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }

        if ( response == null )
        {
            if ( json != null )
            {
                response = formatOkResponseWithJsonEntity( json );
            }
            else
            {
                response = Response.status( Status.NO_CONTENT )
                                   .build();
            }
        }

        return response;
    }

    @Path( "/{groupId}/{artifactId}/{version}/dependencies" )
    @GET
    public Response dependenciesOf( @PathParam( "groupId" ) final String gid,
                                    @PathParam( "artifactId" ) final String aid,
                                    @PathParam( "version" ) final String ver, @QueryParam( "wsid" ) final String wsid,
                                    final @QueryParam( "scopes" ) String scopes )
    {
        Response response = null;
        String json = null;
        try
        {
            json = controller.dependenciesOf( gid, aid, ver, wsid, DependencyScope.parseScopes( scopes ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }

        if ( response == null )
        {
            if ( json != null )
            {
                response = formatOkResponseWithJsonEntity( json );
            }
            else
            {
                response = Response.status( Status.NO_CONTENT )
                                   .build();
            }
        }
        return response;
    }

    @Path( "/{groupId}/{artifactId}/{version}/plugins" )
    @GET
    public Response pluginsOf( @PathParam( "groupId" ) final String gid, @PathParam( "artifactId" ) final String aid,
                               @PathParam( "version" ) final String ver, @QueryParam( "wsid" ) final String wsid )
    {
        Response response = null;
        String json = null;
        try
        {
            json = controller.relationshipsDeclaredBy( gid, aid, ver, wsid, RelationshipType.PLUGIN );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }

        if ( response == null )
        {
            if ( json != null )
            {
                response = formatOkResponseWithJsonEntity( json );
            }
            else
            {
                response = Response.status( Status.NO_CONTENT )
                                   .build();
            }
        }
        return response;
    }

    @Path( "/{groupId}/{artifactId}/{version}/extensions" )
    @GET
    public Response extensionsOf( @PathParam( "groupId" ) final String gid,
                                  @PathParam( "artifactId" ) final String aid,
                                  @PathParam( "version" ) final String ver, @QueryParam( "wsid" ) final String wsid )
    {
        Response response = null;
        String json = null;
        try
        {
            json = controller.relationshipsDeclaredBy( gid, aid, ver, wsid, RelationshipType.EXTENSION );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }

        if ( response == null )
        {
            if ( json != null )
            {
                response = formatOkResponseWithJsonEntity( json );
            }
            else
            {
                response = Response.status( Status.NO_CONTENT )
                                   .build();
            }
        }
        return response;
    }

    @Path( "/{groupId}/{artifactId}/{version}/relationships" )
    @GET
    public Response relationshipsSpecifiedBy( @PathParam( "groupId" ) final String gid,
                                              @PathParam( "artifactId" ) final String aid,
                                              @PathParam( "version" ) final String ver,
                                              @QueryParam( "wsid" ) final String wsid,
                                              @Context final HttpServletRequest request )
    {
        Response response = null;
        String json = null;
        try
        {
            json = controller.relationshipsTargeting( gid, aid, ver, wsid, parseQueryMap( request.getQueryString() ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }

        if ( response == null )
        {
            if ( json != null )
            {
                response = formatOkResponseWithJsonEntity( json );
            }
            else
            {
                response = Response.status( Status.NO_CONTENT )
                                   .build();
            }
        }
        return response;
    }

    @Path( "/{groupId}/{artifactId}/{version}/users" )
    @GET
    public Response relationshipsTargeting( @PathParam( "groupId" ) final String gid,
                                            @PathParam( "artifactId" ) final String aid,
                                            @PathParam( "version" ) final String ver,
                                            @QueryParam( "wsid" ) final String wsid,
                                            @Context final HttpServletRequest request )
    {
        Response response = null;
        String json = null;
        try
        {
            json = controller.relationshipsTargeting( gid, aid, ver, wsid, parseQueryMap( request.getQueryString() ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }

        if ( response == null )
        {
            if ( json != null )
            {
                response = formatOkResponseWithJsonEntity( json );
            }
            else
            {
                response = Response.status( Status.NO_CONTENT )
                                   .build();
            }
        }

        return response;
    }

}
