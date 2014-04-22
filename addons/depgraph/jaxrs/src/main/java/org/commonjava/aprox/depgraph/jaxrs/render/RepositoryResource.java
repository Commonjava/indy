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

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.bind.jaxrs.util.JaxRsUriFormatter;
import org.commonjava.aprox.depgraph.rest.RepositoryController;
import org.commonjava.aprox.util.ApplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/depgraph/repo" )
public class RepositoryResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private RepositoryController controller;

    @POST
    @Path( "/urlmap" )
    @Produces( "application/json" )
    public Response getUrlMap( @Context final HttpServletRequest req, @Context final HttpServletResponse resp, @Context final UriInfo uriInfo )
    {
        try
        {
            final String baseUri = uriInfo.getAbsolutePath()
                                          .toString();

            final String json = controller.getUrlMap( req.getInputStream(), baseUri, new JaxRsUriFormatter( uriInfo ) );
            return Response.ok( json )
                           .type( "application/json" )
                           .build();
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to retrieve request input stream: %s", e.getMessage() ), e );
            return AproxExceptionUtils.formatResponse( ApplicationStatus.BAD_REQUEST, e );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @POST
    @Path( "/downlog" )
    @Produces( "text/plain" )
    public Response getDownloadLog( @Context final HttpServletRequest req, @Context final HttpServletResponse resp, @Context final UriInfo uriInfo )
    {
        try
        {
            final String baseUri = uriInfo.getAbsolutePath()
                                          .toString();

            final String downlog = controller.getDownloadLog( req.getInputStream(), baseUri, new JaxRsUriFormatter( uriInfo ) );
            return Response.ok( downlog )
                           .type( "text/plain" )
                           .build();
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to retrieve request input stream: %s", e.getMessage() ), e );
            return AproxExceptionUtils.formatResponse( ApplicationStatus.BAD_REQUEST, e );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    @POST
    @Path( "/zip" )
    @Produces( "application/zip" )
    public Response getZipRepository( @Context final HttpServletRequest req, @Context final HttpServletResponse resp )
    {
        try
        {
            controller.getZipRepository( req.getInputStream(), resp.getOutputStream() );

        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to retrieve request input stream and/or response output stream: %s", e.getMessage() ), e );
            return AproxExceptionUtils.formatResponse( ApplicationStatus.BAD_REQUEST, e );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }

        return Response.ok()
                       .type( "application/zip" )
                       .build();
    }

}
