/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.depgraph.jaxrs;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.depgraph.rest.MetadataController;
import org.commonjava.aprox.util.ApplicationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/api/depgraph/meta" )
@ApplicationScoped
public class MetadataResource
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private MetadataController controller;

    @Path( "/batch" )
    @POST
    @Consumes( ApplicationContent.application_json )
    public Response batchUpdate( @QueryParam( "wsid" ) final String wsid, @Context final HttpServletRequest request )
    {
        Response response = null;
        try
        {
            // FIXME: Figure out character encoding parse.
            controller.batchUpdate( request.getInputStream(), request.getCharacterEncoding(), wsid );
            response = Response.ok()
                               .build();
        }
        catch ( final AproxWorkflowException | IOException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/for/{groupId}/{artifactId}/{version}" )
    @GET
    public Response getMetadata( @PathParam( "groupId" ) final String gid, @PathParam( "artifactId" ) final String aid,
                                 @PathParam( "version" ) final String ver, @QueryParam( "wsid" ) final String wsid )
    {
        Response response = null;
        String json = null;
        try
        {
            json = controller.getMetadata( gid, aid, ver, wsid );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }

        if ( response == null )
        {
            if ( json == null )
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }
            else
            {
                response = formatOkResponseWithJsonEntity( json );
            }
        }

        return response;
    }

    @Path( "/forkey/{groupId}/{artifactId}/{version}/{key}" )
    public Response getMetadataValue( @PathParam( "groupId" ) final String gid,
                                      @PathParam( "artifactId" ) final String aid,
                                      @PathParam( "version" ) final String ver,
                                      @QueryParam( "wsid" ) final String wsid, @PathParam( "key" ) final String k )
    {
        Response response = null;
        String json = null;
        try
        {
            json = controller.getMetadataValue( gid, aid, ver, k, wsid );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }

        if ( response == null )
        {
            if ( json == null )
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }
            else
            {
                response = formatOkResponseWithJsonEntity( json );
            }
        }

        return response;
    }

    @Path( "/{groupId}/{artifactId}/{version}" )
    @POST
    public Response updateMetadata( @PathParam( "groupId" ) final String gid,
                                    @PathParam( "artifactId" ) final String aid,
                                    @PathParam( "version" ) final String ver, @QueryParam( "wsid" ) final String wsid,
                                    @Context final HttpServletRequest request )
    {
        Response response = null;
        try
        {
            // FIXME: Figure out character encoding parse.
            controller.updateMetadata( gid, aid, ver, request.getInputStream(), request.getCharacterEncoding(), wsid );
            response = Response.ok()
                               .build();
        }
        catch ( final AproxWorkflowException | IOException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/collate" )
    @POST
    public Response getCollation( @Context final HttpServletRequest request )
    {
        Response response = null;
        String json = null;
        try
        {
            // FIXME: Figure out character encoding parse.
            json = controller.getCollation( request.getInputStream(), request.getCharacterEncoding() );
        }
        catch ( final AproxWorkflowException | IOException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }

        if ( response == null )
        {
            if ( json == null )
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }
            else
            {
                response = formatOkResponseWithJsonEntity( json );
            }
        }

        return response;
    }
}
