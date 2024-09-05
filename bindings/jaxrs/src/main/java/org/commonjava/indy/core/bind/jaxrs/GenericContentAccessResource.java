/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.bind.jaxrs;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.commonjava.indy.bind.jaxrs.util.REST;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import static org.commonjava.indy.IndyContentConstants.CHECK_CACHE_ONLY;
import static org.commonjava.indy.content.DownloadManager.ROOT_PATH;
import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_CONTENT_REST_BASE_PATH;
import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;

@Api( value = "generic-http Content Access and Storage" )
@Path( "/api/content/generic-http/{type: (hosted|group|remote)}/{name}" )
@ApplicationScoped
@REST
public class GenericContentAccessResource
                implements PackageContentAccessResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentAccessHandler handler;

    public GenericContentAccessResource()
    {
    }

    public GenericContentAccessResource( final ContentAccessHandler handler )
    {
        this.handler = handler;
    }

    @Override
    @ApiOperation( "Store content under the given artifact store (type/name) and path." )
    @ApiResponses( { @ApiResponse( code = 201, message = "Content was stored successfully" ),
        @ApiResponse( code = 400, message = "No appropriate storage location found in the specified store." ) } )
    @PUT
    @Path( "/{path: (.+)?}" )
    public Response doCreate(
            final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
            final @ApiParam( required = true ) @PathParam( "name" ) String name,
            final @PathParam( "path" ) String path, final @Context UriInfo uriInfo,
            final @Context HttpServletRequest request )
    {
        Class cls = GenericContentAccessResource.class;
        return handler.doCreate( GENERIC_PKG_KEY, type, name, path, request,
                                 new EventMetadata(), () -> uriInfo.getBaseUriBuilder()
                                                                   .path( cls )
                                                                   .path( path )
                                                                   .build( GENERIC_PKG_KEY, type, name ) );
    }

    @ApiOperation( "Store '/' in the given artifact store by handling the '/' as special filepath" )
    @PUT
    @Path( "/" )
    public Response doCreate(
            final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
            final @ApiParam( required = true ) @PathParam( "name" ) String name, final @Context UriInfo uriInfo,
            final @Context HttpServletRequest request )
    {
        return doCreate( type, name, ROOT_PATH, uriInfo, request );
    }

    @Override
    @ApiOperation( "Delete content under the given store (type/name) and path." )
    @ApiResponses( { @ApiResponse( code = 404, message = "Content is not available" ),
                    @ApiResponse( code = 204, message = "Content was deleted successfully" ) } )
    @DELETE
    @Path( "/{path: (.*)}" )
    public Response doDelete(
            final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
            final @ApiParam( required = true ) @PathParam( "name" ) String name,
            final @PathParam( "path" ) String path,
            final @ApiParam( name = "cache-only", value = "true or false" ) @QueryParam( CHECK_CACHE_ONLY ) Boolean cacheOnly )
    {
        return handler.doDelete( GENERIC_PKG_KEY, type, name, path, new EventMetadata() );
    }

    @Override
    @ApiOperation( "Check content under the given store and path." )
    @ApiResponses( { @ApiResponse( code = 404, message = "Content is not available" ),
                    @ApiResponse( code = 200, message = "Get header metadata for content" ), } )
    @HEAD
    @Path( "/{path: (.*)}" )
    public Response doHead(
            final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
            final @ApiParam( required = true ) @PathParam( "name" ) String name,
            final @PathParam( "path" ) String path, final @QueryParam( CHECK_CACHE_ONLY ) Boolean cacheOnly,
            @Context final UriInfo uriInfo, @Context final HttpServletRequest request )
    {
        final String baseUri = uriInfo.getBaseUriBuilder().path( GENERIC_CONTENT_REST_BASE_PATH ).build().toString();
        return handler.doHead( GENERIC_PKG_KEY, type, name, path, cacheOnly, baseUri, request, new EventMetadata(),
                               null, false );
    }

    @ApiOperation( "Check '/' in the given artifact store by handling the '/' as special filepath" )
    @HEAD
    @Path( "/" )
    public Response doHead(
            final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
            final @ApiParam( required = true ) @PathParam( "name" ) String name,
            final @QueryParam( CHECK_CACHE_ONLY ) Boolean cacheOnly, @Context final UriInfo uriInfo,
            @Context final HttpServletRequest request )
    {
        return doHead( type, name, ROOT_PATH, cacheOnly, uriInfo, request );
    }

    @Override
    @ApiOperation( "Retrieve Maven artifact content under the given artifact store (type/name) and path." )
    @ApiResponses( { @ApiResponse( code = 404, message = "Content is not available" ),
                    @ApiResponse( code = 200, response = StreamingOutput.class, message = "Content stream" ), } )
    @GET
    @Path( "/{path: (.*)}" )
    public Response doGet(
            final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
            final @ApiParam( required = true ) @PathParam( "name" ) String name,
            final @PathParam( "path" ) String path, @Context final UriInfo uriInfo,
            @Context final HttpServletRequest request )
    {
        final String baseUri = uriInfo.getBaseUriBuilder().path( GENERIC_CONTENT_REST_BASE_PATH ).build().toString();
        return handler.doGet( GENERIC_PKG_KEY, type, name, path, baseUri, request, new EventMetadata(), null, false );
    }

    @Override
    @ApiOperation( "Retrieve '/' in the given artifact store by handling the '/' as special filepath" )
    @GET
    @Path( "/" )
    public Response doGet(
            final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
            final @ApiParam( required = true ) @PathParam( "name" ) String name, @Context final UriInfo uriInfo,
            @Context final HttpServletRequest request )
    {
        return doGet( type, name, ROOT_PATH, uriInfo, request );
    }

}
