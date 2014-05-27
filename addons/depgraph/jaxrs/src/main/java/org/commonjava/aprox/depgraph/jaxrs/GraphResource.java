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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.depgraph.rest.GraphController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/depgraph/rel" )
@Produces( MediaType.APPLICATION_JSON )
@RequestScoped
public class GraphResource
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private GraphController controller;

    @Context
    private UriInfo info;

    @Path( "/reindex{gav: ([^/]+/[^/]+/[^/]+)?}" )
    @GET
    public Response reindex( @PathParam( "gav" ) final String gav, @Context final HttpServletRequest request )
    {
        try
        {
            controller.reindex( gav, getWorkspaceId( info ) );
            return Response.ok()
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @Path( "/errors{gav: ([^/]+/[^/]+/[^/]+)?}" )
    @GET
    public Response errors( @PathParam( "gav" ) final String gav, @Context final HttpServletRequest request )
    {
        try
        {
            final String json = controller.errors( gav, getWorkspaceId( info ) );
            if ( json != null )
            {
                return Response.ok( json )
                               .build();
            }
            else
            {
                return Response.ok()
                               .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @Path( "/incomplete{gav: ([^/]+/[^/]+/[^/]+)?}" )
    @GET
    public Response incomplete( @PathParam( "gav" ) final String gav, @Context final HttpServletRequest request )
    {
        try
        {
            final String json = controller.incomplete( gav, getWorkspaceId( info ), request.getParameterMap() );

            return json == null ? Response.ok()
                                          .build() : Response.ok( json )
                                                             .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @Path( "/variable{gav: (.+/.+/.+)?}" )
    @GET
    public Response variable( @PathParam( "gav" ) final String gav, @Context final HttpServletRequest request )
    {
        try
        {
            final String json = controller.variable( gav, getWorkspaceId( info ), request.getParameterMap() );

            return json == null ? Response.ok()
                                          .build() : Response.ok( json )
                                                             .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @Path( "/ancestry/{g}/{a}/{v}" )
    @GET
    public Response ancestryOf( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                                @PathParam( "v" ) final String version )
    {
        try
        {
            final String json = controller.ancestryOf( groupId, artifactId, version, getWorkspaceId( info ) );

            return json == null ? Response.ok()
                                          .build() : Response.ok( json )
                                                             .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @Path( "/build-order/{g}/{a}/{v}" )
    @GET
    public Response buildOrder( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                                @PathParam( "v" ) final String version, @Context final HttpServletRequest request )
    {
        try
        {
            final String json =
                controller.buildOrder( groupId, artifactId, version, getWorkspaceId( info ), request.getParameterMap() );

            return json == null ? Response.ok()
                                          .build() : Response.ok( json )
                                                             .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @Path( "/project/{g}/{a}/{v}" )
    @GET
    public Response projectGraph( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                                  @PathParam( "v" ) final String version, @Context final HttpServletRequest request )
    {
        try
        {
            final String json =
                controller.projectGraph( groupId, artifactId, version, getWorkspaceId( info ),
                                         request.getParameterMap() );

            return json == null ? Response.ok()
                                          .build() : Response.ok( json )
                                                             .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

}
