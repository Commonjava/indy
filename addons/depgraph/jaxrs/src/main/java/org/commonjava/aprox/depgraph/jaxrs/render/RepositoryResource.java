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
package org.commonjava.aprox.depgraph.jaxrs.render;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.util.ApplicationContent.application_json;
import static org.commonjava.aprox.util.ApplicationContent.application_zip;
import static org.commonjava.aprox.util.ApplicationContent.text_plain;

import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.bind.jaxrs.util.JaxRsUriFormatter;
import org.commonjava.aprox.depgraph.rest.RepositoryController;
import org.commonjava.aprox.util.ApplicationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/api/depgraph/repo" )
public class RepositoryResource
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private RepositoryController controller;

    @Path( "/urlmap" )
    @POST
    @Consumes( application_json )
    @Produces( application_json )
    public Response getUrlMap( final @Context UriInfo uriInfo, final @Context HttpServletRequest request )
    {
        Response response = null;
        try
        {
            final String baseUri = uriInfo.getAbsolutePathBuilder()
                                          .path( "api" )
                                          .build()
                                          .toString();

            final String json = controller.getUrlMap( request.getInputStream(), baseUri, new JaxRsUriFormatter() );

            if ( json == null )
            {
                response = Response.noContent()
                                   .build();
            }
            else
            {
                response = formatOkResponseWithJsonEntity( json );
            }
        }
        catch ( final AproxWorkflowException | IOException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/paths" )
    @POST
    @Consumes( application_json )
    @Produces( application_json )
    public Response getPaths( final @Context UriInfo uriInfo, final @Context HttpServletRequest request )
    {
        Response response = null;
        try
        {
            final String baseUri = uriInfo.getAbsolutePathBuilder()
                                          .path( "api" )
                                          .build()
                                          .toString();

            final String json = controller.getPaths( request.getInputStream(), new JaxRsUriFormatter() );

            if ( json == null )
            {
                response = Response.noContent()
                                   .build();
            }
            else
            {
                response = formatOkResponseWithJsonEntity( json );
            }
        }
        catch ( final AproxWorkflowException | IOException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/downlog" )
    @POST
    @Consumes( application_json )
    @Produces( text_plain )
    public Response getDownloadLog( final @Context UriInfo uriInfo, final @Context HttpServletRequest request )
    {
        Response response = null;
        try
        {
            final String baseUri = uriInfo.getAbsolutePathBuilder()
                                          .path( "api" )
                                          .build()
                                          .toString();

            final String downlog =
                controller.getDownloadLog( request.getInputStream(), baseUri, new JaxRsUriFormatter() );
            if ( downlog == null )
            {
                response = Response.noContent()
                                   .build();
            }
            else
            {
                response = formatOkResponseWithEntity( downlog, ApplicationContent.text_plain );
            }
        }
        catch ( final AproxWorkflowException | IOException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/zip" )
    @POST
    @Consumes( application_json )
    @Produces( application_zip )
    public Response getZipRepository( final @Context HttpServletRequest request, final @Context HttpServletResponse resp )
    {
        final StreamingOutput out = new StreamingOutput()
        {
            @Override
            public void write( final OutputStream output )
                throws IOException, WebApplicationException
            {
                try
                {
                    controller.getZipRepository( request.getInputStream(), output );
                }
                catch ( final AproxWorkflowException e )
                {
                    throw new WebApplicationException( formatResponse( e, true ) );
                }
            }
        };

        return Response.ok( out )
                       .build();
    }
}
