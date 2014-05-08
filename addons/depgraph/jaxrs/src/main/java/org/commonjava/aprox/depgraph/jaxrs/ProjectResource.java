/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.depgraph.jaxrs;

import static org.commonjava.aprox.depgraph.jaxrs.util.DepgraphParamUtils.getWorkspaceId;

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
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.depgraph.rest.ProjectController;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/depgraph/project" )
@Produces( MediaType.APPLICATION_JSON )
@RequestScoped
public class ProjectResource
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ProjectController controller;

    @Context
    private UriInfo info;

    @Path( "/{g}/{a}/{v}/errors" )
    @GET
    public Response errors( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                            @PathParam( "v" ) final String version )
    {
        String json;
        try
        {
            json = controller.errors( groupId, artifactId, version, getWorkspaceId( info ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }

        return json == null ? Response.noContent()
                                      .build() : Response.ok( json )
                                                         .build();
    }

    @Path( "/list" )
    @GET
    public Response list( @QueryParam( "g" ) final String groupIdPattern,
                          @QueryParam( "a" ) final String artifactIdPattern )
    {
        String json;
        try
        {
            json = controller.list( groupIdPattern, artifactIdPattern, getWorkspaceId( info ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }

        return json == null ? Response.noContent()
                                      .build() : Response.ok( json )
                                                         .build();
    }

    @Path( "/{g}/{a}/{v}/parent" )
    @GET
    public Response parentOf( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                              @PathParam( "v" ) final String version )
    {
        String json;
        try
        {
            json = controller.parentOf( groupId, artifactId, version, getWorkspaceId( info ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }

        return json == null ? Response.noContent()
                                      .build() : Response.ok( json )
                                                         .build();
    }

    @Path( "/{g}/{a}/{v}/dependencies" )
    @GET
    public Response dependenciesOf( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                                    @PathParam( "v" ) final String version,
                                    @QueryParam( "scopes" ) final String scopesStr )
    {
        String json;
        try
        {
            json =
                controller.dependenciesOf( groupId, artifactId, version, getWorkspaceId( info ),
                                           DependencyScope.parseScopes( scopesStr ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }

        return json == null ? Response.noContent()
                                      .build() : Response.ok( json )
                                                         .build();
    }

    @Path( "/{g}/{a}/{v}/plugins" )
    @GET
    public Response pluginsOf( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                               @PathParam( "v" ) final String version )
    {
        String json;
        try
        {
            json =
                controller.relationshipsDeclaredBy( groupId, artifactId, version, getWorkspaceId( info ),
                                                    RelationshipType.PLUGIN );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }

        return json == null ? Response.noContent()
                                      .build() : Response.ok( json )
                                                         .build();
    }

    @Path( "/{g}/{a}/{v}/extensions" )
    @GET
    public Response extensionsOf( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                                  @PathParam( "v" ) final String version )
    {
        String json;
        try
        {
            json =
                controller.relationshipsDeclaredBy( groupId, artifactId, version, getWorkspaceId( info ),
                                                    RelationshipType.PLUGIN );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }

        return json == null ? Response.noContent()
                                      .build() : Response.ok( json )
                                                         .build();
    }

    @Path( "/{g}/{a}/{v}/relationships" )
    @GET
    public Response relationshipsSpecifiedBy( @PathParam( "g" ) final String groupId,
                                              @PathParam( "a" ) final String artifactId,
                                              @PathParam( "v" ) final String version,
                                              @Context final HttpServletRequest request )
    {
        String json;
        try
        {
            json =
                controller.relationshipsTargeting( groupId, artifactId, version, getWorkspaceId( info ),
                                                   request.getParameterMap() );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }

        return json == null ? Response.noContent()
                                      .build() : Response.ok( json )
                                                         .build();
    }

    @Path( "/{g}/{a}/{v}/users" )
    @GET
    public Response relationshipsTargeting( @PathParam( "g" ) final String groupId,
                                            @PathParam( "a" ) final String artifactId,
                                            @PathParam( "v" ) final String version,
                                            @Context final HttpServletRequest request )
    {
        String json;
        try
        {
            json =
                controller.relationshipsTargeting( groupId, artifactId, version, getWorkspaceId( info ),
                                                   request.getParameterMap() );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }

        return json == null ? Response.noContent()
                                      .build() : Response.ok( json )
                                                         .build();
    }

}
