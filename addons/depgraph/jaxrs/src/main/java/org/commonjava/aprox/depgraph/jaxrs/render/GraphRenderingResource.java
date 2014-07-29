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
package org.commonjava.aprox.depgraph.jaxrs.render;

import static org.commonjava.aprox.depgraph.jaxrs.util.DepgraphParamUtils.getWorkspaceId;

import java.io.File;
import java.io.IOException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.depgraph.rest.RenderingController;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.aprox.util.ApplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/depgraph/render" )
@RequestScoped
public class GraphRenderingResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private RenderingController controller;

    @Context
    private UriInfo info;

    @Path( "/bom/{g}/{a}/{v}" )
    @Produces( "application/xml" )
    @Consumes( "application/json" )
    @POST
    @Deprecated
    public Response bomFor( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                            @PathParam( "v" ) final String version, @Context final HttpServletRequest request )
    {
        try
        {
            final String out =
                controller.bomFor( groupId, artifactId, version, getWorkspaceId( info ), request.getParameterMap(),
                                   request.getInputStream() );

            return Response.ok( out )
                           .header( ApplicationHeader.deprecated.key(), "/depgraph/render/bom" )
                           .build();

        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to get servlet request input stream: %s", e.getMessage() ), e );
            return AproxExceptionUtils.formatResponse( ApplicationStatus.BAD_REQUEST, e );
        }
    }

    @Path( "/bom" )
    @Consumes( "application/json" )
    @Produces( "application/xml" )
    @POST
    public Response bomForDTO( @Context final HttpServletRequest request )
    {
        try
        {
            final String out = controller.bomFor( request.getInputStream() );

            return Response.ok( out )
                           .build();

        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to get servlet request input stream: %s", e.getMessage() ), e );
            return AproxExceptionUtils.formatResponse( ApplicationStatus.BAD_REQUEST, e );
        }
    }

    @Path( "/dotfile/{g}/{a}/{v}" )
    @Produces( "text/x-graphviz" )
    @GET
    public Response dotfile( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                             @PathParam( "v" ) final String version, @Context final HttpServletRequest request )
    {
        try
        {
            final String dotfile =
                controller.dotfile( groupId, artifactId, version, getWorkspaceId( info ), request.getParameterMap() );
            return Response.ok( dotfile )
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @Path( "/tree" )
    @Produces( MediaType.TEXT_PLAIN )
    @POST
    public Response tree( @Context final HttpServletRequest req )
    {
        try
        {
            final File treeOutput = controller.tree( req.getInputStream() );
            return Response.ok( treeOutput )
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to retrieve request input stream: %s", e.getMessage() ), e );
            return AproxExceptionUtils.formatResponse( ApplicationStatus.BAD_REQUEST, e );
        }
    }
}
