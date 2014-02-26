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
package org.commonjava.aprox.depgraph.jaxrs.resolve;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
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

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.depgraph.rest.ResolverController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/depgraph/resolve/{from: (.+)}" )
@Produces( MediaType.APPLICATION_JSON )
@ApplicationScoped
public class ResolverResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ResolverController controller;

    @Path( "/{g}/{a}/{v}" )
    @GET
    public Response resolveGraph( @PathParam( "from" ) final String from, @PathParam( "g" ) final String groupId,
                                  @PathParam( "a" ) final String artifactId, @PathParam( "v" ) final String version,
                                  @Context final HttpServletRequest request, @QueryParam( "recurse" ) @DefaultValue( "true" ) final boolean recurse )
    {
        try
        {
            final String json = controller.resolveGraph( from, groupId, artifactId, version, recurse, request.getParameterMap() );
            return Response.ok( json )
                           .build();

        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @Path( "/{g}/{a}/{v}/incomplete" )
    @GET
    public Response resolveIncomplete( @PathParam( "from" ) final String from, @PathParam( "g" ) final String groupId,
                                       @PathParam( "a" ) final String artifactId, @PathParam( "v" ) final String version,
                                       @QueryParam( "recurse" ) @DefaultValue( "false" ) final boolean recurse,
                                       @Context final HttpServletRequest request, @Context final HttpServletResponse resp )
        throws IOException
    {
        try
        {
            final String json = controller.resolveIncomplete( from, groupId, artifactId, version, recurse, request.getParameterMap() );
            return Response.ok( json )
                           .build();

        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

}
