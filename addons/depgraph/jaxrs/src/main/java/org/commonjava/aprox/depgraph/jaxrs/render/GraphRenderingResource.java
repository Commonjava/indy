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

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.model.util.HttpUtils.parseQueryMap;
import static org.commonjava.aprox.util.ApplicationContent.application_xml;
import static org.commonjava.aprox.util.ApplicationContent.text_plain;

import java.io.File;
import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.depgraph.rest.RenderingController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/api/depgraph/render" )
@ApplicationScoped
public class GraphRenderingResource
    implements AproxResources
{

    private static final String TYPE_GRAPHVIZ = "text/x-graphviz";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private RenderingController controller;

    @Path( "/bom/{groupId}/{artifactId}/{version}" )
    @POST
    @Produces( application_xml )
    @Deprecated
    public Response bomFor( final @PathParam( "groupId" ) String gid, @PathParam( "artifactId" ) final String aid,
                            @PathParam( "version" ) final String ver, @QueryParam( "wsid" ) final String wsid,
                            @Context final HttpServletRequest request )
    {
        Response response = null;

        try
        {
            final String out =
                controller.bomFor( gid, aid, ver, wsid, parseQueryMap( request.getQueryString() ),
                                   request.getInputStream() );
            if ( out == null )
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }
            else
            {
                response = formatOkResponseWithEntity( out, application_xml );
            }

        }
        catch ( final AproxWorkflowException | IOException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/bom" )
    @POST
    @Produces( application_xml )
    public Response bomForDTO( @Context final HttpServletRequest request )
    {
        Response response = null;
        try
        {
            final String out = controller.bomFor( request.getInputStream() );
            if ( out == null )
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }
            else
            {
                response = formatOkResponseWithEntity( out, application_xml );
            }

        }
        catch ( final AproxWorkflowException | IOException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/dotfile/{groupId}/{artifactId}/{version}" )
    @GET
    @Produces( TYPE_GRAPHVIZ )
    public Response dotfile( final @PathParam( "groupId" ) String gid, @PathParam( "artifactId" ) final String aid,
                             @PathParam( "version" ) final String ver, @QueryParam( "wsid" ) final String wsid,
                             @Context final HttpServletRequest request )
    {
        Response response = null;
        try
        {
            final String out = controller.dotfile( gid, aid, ver, wsid, parseQueryMap( request.getQueryString() ) );
            if ( out == null )
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }
            else
            {
                response = formatOkResponseWithEntity( out, TYPE_GRAPHVIZ );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/tree" )
    @POST
    @Produces( text_plain )
    public Response tree( @Context final HttpServletRequest request )
    {
        Response response = null;
        try
        {
            final File out = controller.tree( request.getInputStream() );
            if ( out == null )
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }
            else
            {
                response = formatOkResponseWithEntity( out, text_plain );
            }
        }
        catch ( final AproxWorkflowException | IOException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }
}
