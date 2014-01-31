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
import javax.ws.rs.core.UriBuilder;

import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.bind.jaxrs.util.JaxRsUriFormatter;
import org.commonjava.aprox.depgraph.rest.RepositoryController;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.util.logging.Logger;

@Path( "/depgraph/repo" )
public class RepositoryResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private RepositoryController controller;

    @POST
    @Path( "/urlmap" )
    @Produces( "application/json" )
    public Response getUrlMap( @Context final HttpServletRequest req, @Context final HttpServletResponse resp, @Context final UriBuilder builder )
    {
        try
        {
            final String json = controller.getUrlMap( req.getInputStream(), new JaxRsUriFormatter( builder ) );
            return Response.ok( json )
                           .type( "application/json" )
                           .build();
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to retrieve request input stream: %s", e, e.getMessage() );
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
    public Response getDownloadLog( @Context final HttpServletRequest req, @Context final HttpServletResponse resp, @Context final UriBuilder builder )
    {
        try
        {
            final String downlog = controller.getDownloadLog( req.getInputStream(), new JaxRsUriFormatter( builder ) );
            return Response.ok( downlog )
                           .type( "text/plain" )
                           .build();
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to retrieve request input stream: %s", e, e.getMessage() );
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
            logger.error( "Failed to retrieve request input stream and/or response output stream: %s", e, e.getMessage() );
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
