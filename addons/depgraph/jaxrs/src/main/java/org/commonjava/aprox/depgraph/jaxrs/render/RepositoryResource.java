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

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.*;
import static org.commonjava.aprox.util.ApplicationContent.application_json;
import static org.commonjava.aprox.util.ApplicationContent.application_zip;
import static org.commonjava.aprox.util.ApplicationContent.text_plain;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.bind.jaxrs.util.JaxRsUriFormatter;
import org.commonjava.aprox.depgraph.dto.DownlogDTO;
import org.commonjava.aprox.depgraph.dto.DownlogRequest;
import org.commonjava.aprox.depgraph.dto.UrlMapDTO;
import org.commonjava.aprox.depgraph.rest.RepositoryController;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.maven.cartographer.request.RepositoryContentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/api/depgraph/repo" )
@Consumes( { "application/json", "application/aprox*+json" } )
public class RepositoryResource
                implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private RepositoryController controller;

    @Path( "/urlmap" )
    @Produces( { "application/json", "application/aprox*+json" } )
    @POST
    public UrlMapDTO getUrlMap( final RepositoryContentRequest request, final @Context UriInfo uriInfo )
    {
        Response response = null;
        try
        {
            final String baseUri = uriInfo.getAbsolutePathBuilder().path( "api" ).build().toString();

            return controller.getUrlMap( request, baseUri, new JaxRsUriFormatter() );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e );
        }

        return null;
    }

    @Path( "/downlog" )
    @POST
    @Produces( text_plain )
    public DownlogDTO getDownloadLog( final DownlogRequest request, final @Context UriInfo uriInfo )
    {
        Response response = null;
        try
        {
            final String baseUri = uriInfo.getAbsolutePathBuilder().path( "api" ).build().toString();

            return controller.getDownloadLog( request, baseUri, new JaxRsUriFormatter() );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }

        return null;
    }

    @Path( "/zip" )
    @POST
    @Produces( application_zip )
    public Response getZipRepository( RepositoryContentRequest request )
    {
        final StreamingOutput out = ( output ) -> {
            try
            {
                controller.getZipRepository( request, output );
            }
            catch ( final AproxWorkflowException e )
            {
                throwError( e );
            }
        };

        return Response.ok( out ).build();
    }
}
