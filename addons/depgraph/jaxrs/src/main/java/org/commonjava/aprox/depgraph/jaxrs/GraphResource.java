/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.depgraph.jaxrs;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.depgraph.rest.GraphController;
import org.commonjava.util.logging.Logger;

@Path( "/depgraph/rel" )
@Produces( MediaType.APPLICATION_JSON )
@ApplicationScoped
public class GraphResource
{
    private final Logger logger = new Logger( getClass() );

    @Inject
    private GraphController controller;

    @Path( "/reindex{gav: ([^/]+/[^/]+/[^/]+)?}" )
    @GET
    public Response reindex( @PathParam( "gav" ) final String gav, @Context final HttpServletRequest request )
    {
        try
        {
            controller.reindex( gav );
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
            final String json = controller.errors( gav );
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
            final String json = controller.incomplete( gav, request.getParameterMap() );

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
            final String json = controller.variable( gav, request.getParameterMap() );

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
            final String json = controller.ancestryOf( groupId, artifactId, version );

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
            final String json = controller.buildOrder( groupId, artifactId, version, request.getParameterMap() );

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
            final String json = controller.projectGraph( groupId, artifactId, version, request.getParameterMap() );

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
