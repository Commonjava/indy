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

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
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

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.depgraph.rest.MetadataController;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.StringFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/depgraph/meta" )
@Consumes( MediaType.APPLICATION_JSON )
@Produces( MediaType.APPLICATION_JSON )
@ApplicationScoped
public class MetadataResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private MetadataController controller;

    @Path( "/batch" )
    @POST
    public Response batchUpdate( @Context final HttpServletRequest request )
    {
        try
        {
            controller.batchUpdate( request.getInputStream(), request.getCharacterEncoding() );
            return Response.ok()
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
        catch ( final IOException e )
        {
            logger.error( "{}", e, new StringFormat( "Failed to retrieve stream for request body: {}", e.getMessage() ) );
            return AproxExceptionUtils.formatResponse( ApplicationStatus.BAD_REQUEST, e );
        }
    }

    @Path( "/for/{g}/{a}/{v}" )
    @GET
    public Response getMetadata( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                                 @PathParam( "v" ) final String version )
    {
        String json;
        try
        {
            json = controller.getMetadata( groupId, artifactId, version );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }

        return json == null ? Response.status( ApplicationStatus.NOT_FOUND.code() )
                                      .build() : Response.ok( json )
                                                         .build();
    }

    @Path( "/forkey/{g}/{a}/{v}/{k}" )
    @GET
    public Response getMetadataValue( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                                      @PathParam( "v" ) final String version, @PathParam( "k" ) final String key )
    {
        String json;
        try
        {
            json = controller.getMetadataValue( groupId, artifactId, version, key );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }

        return json == null ? Response.status( ApplicationStatus.NOT_FOUND.code() )
                                      .build() : Response.ok( json )
                                                         .build();
    }

    @Path( "/{g}/{a}/{v}" )
    @POST
    public Response updateMetadata( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                                    @PathParam( "v" ) final String version, @Context final HttpServletRequest request )
    {
        try
        {
            controller.updateMetadata( groupId, artifactId, version, request.getInputStream(), request.getCharacterEncoding() );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
        catch ( final IOException e )
        {
            logger.error( "{}", e, new StringFormat( "Failed to retrieve stream for request body: {}", e.getMessage() ) );
            return AproxExceptionUtils.formatResponse( ApplicationStatus.BAD_REQUEST, e );
        }

        return Response.ok()
                       .build();
    }

    @Path( "/collate" )
    @POST
    public Response getCollation( @Context final HttpServletRequest req )
    {
        String json;
        try
        {
            json = controller.getCollation( req.getInputStream(), req.getCharacterEncoding() );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
        catch ( final IOException e )
        {
            logger.error( "{}", e, new StringFormat( "Failed to retrieve stream for request body: {}", e.getMessage() ) );
            return AproxExceptionUtils.formatResponse( ApplicationStatus.BAD_REQUEST, e );
        }

        return json == null ? Response.status( ApplicationStatus.NOT_FOUND.code() )
                                      .build() : Response.ok( json )
                                                         .build();
    }
}
