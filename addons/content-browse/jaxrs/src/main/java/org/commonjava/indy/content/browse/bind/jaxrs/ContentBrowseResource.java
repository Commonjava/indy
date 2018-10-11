/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.content.browse.bind.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.ResponseUtils;
import org.commonjava.indy.content.browse.ContentBrowseController;
import org.commonjava.indy.content.browse.model.ContentBrowseResult;
import org.commonjava.indy.model.core.PackageTypes;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.util.ApplicationContent;
import org.commonjava.indy.util.UriFormatter;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.net.URI;

import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;

@Api( value = "Indy Directory Content Browse", description = "Browse directory content in indy repository" )
@Path( "/api/browse/{packageType}/{type: (hosted|group|remote)}/{name}" )
@ApplicationScoped
public class ContentBrowseResource
        implements IndyResources
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    private static final String BROWSE_REST_BASE_PATH = "/api/browse";

    private static final String CONTENT_REST_BASE_PATH = "/api/content";

    @Inject
    private ContentBrowseController controller;

    @Inject
    private ObjectMapper mapper;

    @Inject
    private UriFormatter uriFormatter;

    @ApiOperation( "Retrieve directory content under the given artifact store (type/name) and directory path." )
    @ApiResponses( { @ApiResponse( code = 404, message = "Content is not available" ),
                           @ApiResponse( code = 200, response = String.class, message = "Rendered content listing" ) } )
    @GET
    @Path( "/{path: (.*)}" )
    @Produces( ApplicationContent.application_json )
    public Response browseDirectory(
            final @ApiParam( allowableValues = "maven,npm", required = true ) @PathParam( "packageType" )
                    String packageType,
            final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" )
                    String type, final @ApiParam( required = true ) @PathParam( "name" ) String name,
            final @PathParam( "path" ) String path, @Context final UriInfo uriInfo )
    {

        return processRequest( packageType, type, name, path, uriInfo );

    }

    @ApiOperation( "Retrieve root listing under the given artifact store (type/name)." )
    @ApiResponses( { @ApiResponse( code = 200, response = String.class, message = "Rendered root content listing" ) } )
    @GET
    @Path( "/" )
    @Produces( ApplicationContent.application_json )
    public Response browseRoot(
            final @ApiParam( allowableValues = "maven,npm", required = true ) @PathParam( "packageType" )
                    String packageType,
            final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" )
                    String type, final @ApiParam( required = true ) @PathParam( "name" ) String name,
            @Context final UriInfo uriInfo )
    {
        return processRequest( packageType, type, name, "", uriInfo );
    }

    private Response processRequest( final String packageType, final String type, final String name, final String path,
                                     final UriInfo uriInfo )
    {
        if ( !PackageTypes.contains( packageType ) )
        {
            return Response.status( 400 ).build();
        }

        final StoreKey sk = new StoreKey( packageType, StoreType.get( type ), name );


        final String browseBaseUri =
                uriInfo.getBaseUriBuilder().path( BROWSE_REST_BASE_PATH + "/" + packageType ).build().toString();

        final String contentBaseUri =
                uriInfo.getBaseUriBuilder().path( CONTENT_REST_BASE_PATH + "/" + packageType ).build().toString();

        Response response;
        ContentBrowseResult result;
        try
        {
            result = controller.browseContent( sk, path, browseBaseUri, contentBaseUri, uriFormatter,
                                               new EventMetadata() );

            response = formatOkResponseWithJsonEntity( result, mapper );
        }
        catch ( IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to list content: %s from: %s. Reason: %s",
                                         StringUtils.isBlank( path ) ? "/" : path, name, e.getMessage() ), e );
            response = ResponseUtils.formatResponse( e );
        }

        return response;
    }
}
