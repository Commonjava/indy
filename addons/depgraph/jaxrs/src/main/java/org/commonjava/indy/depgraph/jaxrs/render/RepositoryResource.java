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
package org.commonjava.indy.depgraph.jaxrs.render;

import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.*;
import static org.commonjava.indy.util.ApplicationContent.application_zip;
import static org.commonjava.indy.util.ApplicationContent.text_plain;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.JaxRsUriFormatter;
import org.commonjava.indy.depgraph.model.DownlogRequest;
import org.commonjava.indy.depgraph.model.DownlogResult;
import org.commonjava.indy.depgraph.model.RepoContentResult;
import org.commonjava.indy.depgraph.model.UrlMapResult;
import org.commonjava.indy.depgraph.rest.RepositoryController;
import org.commonjava.cartographer.request.RepositoryContentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/api/depgraph/repo" )
@Consumes( { "application/json", "application/indy*+json" } )
public class RepositoryResource
                implements IndyResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private RepositoryController controller;

    @Path( "/content" )
    @Produces( { "application/json", "application/indy*+json" } )
    @POST
    public RepoContentResult getRepoContent( final RepositoryContentRequest request, final @Context UriInfo uriInfo )
    {
        try
        {
            final String baseUri = uriInfo.getBaseUriBuilder().path( "api" ).build().toString();

            return controller.getRepoContent( request, baseUri, new JaxRsUriFormatter() );
        }
        catch ( final IndyWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }

        return null;
    }

    @Path( "/urlmap" )
    @Produces( { "application/json", "application/indy*+json" } )
    @POST
    public UrlMapResult getUrlMap( final RepositoryContentRequest request, final @Context UriInfo uriInfo )
    {
        try
        {
            final String baseUri = uriInfo.getBaseUriBuilder().path( "api" ).build().toString();

            return controller.getUrlMap( request, baseUri, new JaxRsUriFormatter() );
        }
        catch ( final IndyWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }

        return null;
    }

    @Path( "/downlog" )
    @POST
    @Produces( text_plain )
    public DownlogResult getDownloadLog( final DownlogRequest request, final @Context UriInfo uriInfo )
    {
        try
        {
            final String baseUri = uriInfo.getBaseUriBuilder().path( "api" ).build().toString();

            return controller.getDownloadLog( request, baseUri, new JaxRsUriFormatter() );
        }
        catch ( final IndyWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }

        return null;
    }

    @Path( "/zip" )
    @POST
    @Produces( application_zip )
    public StreamingOutput getZipRepository( final RepositoryContentRequest request )
    {
        return ( output ) -> {
            try
            {
                controller.getZipRepository( request, output );
            }
            catch ( final IndyWorkflowException e )
            {
                throwError( e );
            }
        };
    }
}
