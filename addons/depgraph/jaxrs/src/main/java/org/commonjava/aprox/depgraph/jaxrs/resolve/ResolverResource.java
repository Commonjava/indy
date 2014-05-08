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
package org.commonjava.aprox.depgraph.jaxrs.resolve;

import static org.commonjava.aprox.depgraph.jaxrs.util.DepgraphParamUtils.getWorkspaceId;

import java.io.IOException;

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
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.depgraph.rest.ResolverController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/depgraph/resolve/{from: (.+)}" )
@Produces( MediaType.APPLICATION_JSON )
@RequestScoped
public class ResolverResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ResolverController controller;

    @Context
    private UriInfo info;

    @Path( "/{g}/{a}/{v}" )
    @GET
    public Response resolveGraph( @PathParam( "from" ) final String from, @PathParam( "g" ) final String groupId,
                                  @PathParam( "a" ) final String artifactId, @PathParam( "v" ) final String version,
                                  @Context final HttpServletRequest request,
                                  @QueryParam( "recurse" ) @DefaultValue( "true" ) final boolean recurse )
    {
        try
        {
            final String json =
                controller.resolveGraph( from, groupId, artifactId, version, recurse, getWorkspaceId( info ),
                                         request.getParameterMap() );
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
                                       @PathParam( "a" ) final String artifactId,
                                       @PathParam( "v" ) final String version,
                                       @QueryParam( "recurse" ) @DefaultValue( "false" ) final boolean recurse,
                                       @Context final HttpServletRequest request,
                                       @Context final HttpServletResponse resp )
        throws IOException
    {
        try
        {
            controller.resolveIncomplete( from, groupId, artifactId, version, recurse, getWorkspaceId( info ),
                                          request.getParameterMap() );
            return Response.ok()
                           .build();

        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

}
