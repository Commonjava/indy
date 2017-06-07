/**
 * Copyright (C) 2011 Red Hat, Inc. (yma@commonjava.org)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.pkg.npm.jaxrs;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.commonjava.indy.core.bind.jaxrs.ContentAccessHandler;
import org.commonjava.indy.core.bind.jaxrs.PackageContentAccessResource;
import org.commonjava.indy.pkg.npm.content.group.PackageMetadataMerger;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_CONTENT_REST_BASE_PATH;
import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;

@Api( value = "NPM Content Access and Storage", description = "Handles retrieval and management of NPM artifact content. This is the main point of access for NPM users." )
@Path( "/api/content/npm/{type: (hosted|group|remote)}/{name}" )
public class NPMContentAccessResource
                implements PackageContentAccessResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String CROSS = "-";

    private static final String PATH_SPLITER = "/";

    private static final String PACAGE_TGZ = "package.tgz";

    @Inject
    private ContentAccessHandler handler;

    public NPMContentAccessResource()
    {
    }

    public NPMContentAccessResource( final ContentAccessHandler handler )
    {
        this.handler = handler;
    }

    /**
     *
     * @param type
     * @param name
     * @param path
     *        Need to make a path format rule for npm package and metadata store. Then it will be easy to do doGet/doHead rest path mapping (package.json and /packageName/-/tarball(s)).
     *        /{packageName}/package.json, /{packageName}/{version}/package.json, /{packageName}/{version}/package.tgz.
     * @param uriInfo
     * @param request
     * @return
     */
    @Override
    @ApiOperation( "Store NPM artifact content under the given artifact store (type/name) and path." )
    @ApiResponses( { @ApiResponse( code = 201, message = "Content was stored successfully" ), @ApiResponse( code = 400, message = "No appropriate storage location was found in the specified store (this store, or a member if a group is specified)." ) } )
    @PUT
    @Path( "/{path: (.+)?}" )
    public Response doCreate( final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                              final @ApiParam( required = true ) @PathParam( "name" ) String name, final @PathParam( "path" ) String path, final @Context UriInfo uriInfo,
                              final @Context HttpServletRequest request )
    {
        return handler.doCreate( NPM_PKG_KEY, type, name, path, request, new EventMetadata(), () -> uriInfo.getBaseUriBuilder()
                                                                                                           .path( getClass() )
                                                                                                           .path( path )
                                                                                                           .build( NPM_PKG_KEY, type, name ) );
    }

    @Override
    @ApiOperation( "Delete NPM package and metadata content under the given artifact store (type/name) and path." )
    @ApiResponses( { @ApiResponse( code = 404, message = "Content is not available" ),
                    @ApiResponse( code = 204, message = "Content was deleted successfully" ) } )
    @DELETE
    @Path( "/{path: (.*)}" )
    public Response doDelete(
                    final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                    final @ApiParam( required = true ) @PathParam( "name" ) String name,
                    final @PathParam( "path" ) String path )
    {
        return handler.doDelete( NPM_PKG_KEY, type, name, path, new EventMetadata() );
    }

    @Override
    @ApiOperation( "Retrieve NPM package header metadata content under the given artifact store (type/name) and packageName." )
    @ApiResponses( { @ApiResponse( code = 404, message = "Metadata Content is not available" ),
                    @ApiResponse( code = 200, message = "Header metadata for package metadata content" ), } )
    @HEAD
    @Path( "/{packageName}" )
    public Response doHead(
                    final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                    final @ApiParam( required = true ) @PathParam( "name" ) String name,
                    final @PathParam( "packageName" ) String packageName,
                    @QueryParam( CHECK_CACHE_ONLY ) final Boolean cacheOnly,
                    @Context final UriInfo uriInfo, @Context final HttpServletRequest request )
    {
        // map /{package} to /{package}/package.json
        final String path = packageName + PATH_SPLITER + PackageMetadataMerger.METADATA_NAME;
        final String baseUri = uriInfo.getBaseUriBuilder().path( NPM_CONTENT_REST_BASE_PATH ).build().toString();
        return handler.doHead( NPM_PKG_KEY, type, name, path, cacheOnly, baseUri, request, new EventMetadata() );
    }

    @ApiOperation( "Retrieve NPM package version header metadata content under the given artifact store (type/name), packageName and version." )
    @ApiResponses( { @ApiResponse( code = 404, message = "Metadata Content is not available" ),
                    @ApiResponse( code = 200, message = "Header metadata for version metadata content" ), } )
    @HEAD
    @Path( "/{packageName}/{version}" )
    public Response doHead(
                    final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                    final @ApiParam( required = true ) @PathParam( "name" ) String name,
                    final @PathParam( "packageName" ) String packageName, final @PathParam( "version" ) String version,
                    @QueryParam( CHECK_CACHE_ONLY ) final Boolean cacheOnly, @Context final UriInfo uriInfo,
                    @Context final HttpServletRequest request )
    {
        // map /{package}/{version} to /{package}/{version}/package.json
        final String path = packageName + PATH_SPLITER + version + PATH_SPLITER + PackageMetadataMerger.METADATA_NAME;
        final String baseUri = uriInfo.getBaseUriBuilder().path( NPM_CONTENT_REST_BASE_PATH ).build().toString();
        return handler.doHead( NPM_PKG_KEY, type, name, path, cacheOnly, baseUri, request,
                               new EventMetadata() );
    }

    @ApiOperation( "Retrieve NPM package tarball header metadata content under the given artifact store (type/name), packageName and tarball." )
    @ApiResponses( { @ApiResponse( code = 404, message = "Content is not available" ),
                    @ApiResponse( code = 200, message = "Header metadata for tarball content" ), } )
    @HEAD
    @Path( "/{packageName}/-/{tarball}" )
    public Response doHead(
                    final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                    final @ApiParam( required = true ) @PathParam( "name" ) String name,
                    final @PathParam( "packageName" ) String packageName,
                    final @ApiParam( allowableValues = CROSS ) @PathParam( "version" ) String version,
                    final @PathParam( "tarball" ) String tarball,
                    @QueryParam( CHECK_CACHE_ONLY ) final Boolean cacheOnly, @Context final UriInfo uriInfo,
                    @Context final HttpServletRequest request )
    {
        // map /{package}/-/{package}-{version}.tgz to /{package}/{version}/package.tgz
        String v = CROSS;
        try
        {
            v = tarball.substring( packageName.length() + CROSS.length(), tarball.length() - 4 );
        }
        catch ( Exception e )
        {
            logger.error( "Version transform error for tarball: %s", tarball );
        }

        final String path = packageName + PATH_SPLITER + v + PATH_SPLITER + PACAGE_TGZ;

        final String baseUri = uriInfo.getBaseUriBuilder().path( NPM_CONTENT_REST_BASE_PATH ).build().toString();
        return handler.doHead( NPM_PKG_KEY, type, name, path, cacheOnly, baseUri, request,
                               new EventMetadata() );
    }

    @Override
    @ApiOperation( "Retrieve NPM package metadata content under the given artifact store (type/name) and packageName." )
    @ApiResponses( { @ApiResponse( code = 404, message = "Metadata content is not available" ),
                    @ApiResponse( code = 200, response = String.class, message = "Rendered metadata content" ), } )
    @GET
    @Path( "/{packageName}" )
    public Response doGet(
                    final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                    final @ApiParam( required = true ) @PathParam( "name" ) String name,
                    final @PathParam( "packageName" ) String packageName, @Context final UriInfo uriInfo,
                    @Context final HttpServletRequest request )
    {
        // map /{package} to /{package}/package.json
        final String path = packageName + PATH_SPLITER + PackageMetadataMerger.METADATA_NAME;

        final String baseUri = uriInfo.getBaseUriBuilder().path( NPM_CONTENT_REST_BASE_PATH ).build().toString();
        return handler.doGet( NPM_PKG_KEY, type, name, path, baseUri, request, new EventMetadata() );
    }

    @ApiOperation( "Retrieve NPM package version metadata content under the given artifact store (type/name), packageName and version." )
    @ApiResponses( { @ApiResponse( code = 404, message = "Metadata content is not available" ),
                    @ApiResponse( code = 200, response = String.class, message = "Rendered metadata content" ), } )
    @GET
    @Path( "/{packageName}/{version}" )
    public Response doGet(
                    final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                    final @ApiParam( required = true ) @PathParam( "name" ) String name,
                    final @PathParam( "packageName" ) String packageName, final @PathParam( "version" ) String version,
                    @Context final UriInfo uriInfo, @Context final HttpServletRequest request )
    {
        // map /{package}/{version} to /{package}/{version}/package.json
        final String path = packageName + PATH_SPLITER + version + PATH_SPLITER + PackageMetadataMerger.METADATA_NAME;

        final String baseUri = uriInfo.getBaseUriBuilder().path( NPM_CONTENT_REST_BASE_PATH ).build().toString();
        return handler.doGet( NPM_PKG_KEY, type, name, path, baseUri, request, new EventMetadata() );
    }

    @ApiOperation( "Retrieve NPM package tarball content under the given artifact store (type/name), packageName and tarball." )
    @ApiResponses( { @ApiResponse( code = 404, message = "Content is not available" ),
                    @ApiResponse( code = 200, response = StreamingOutput.class, message = "Content stream" ), } )
    @GET
    @Path( "/{packageName}/-/{tarball}" )
    public Response doGet(
                    final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                    final @ApiParam( required = true ) @PathParam( "name" ) String name,
                    final @PathParam( "packageName" ) String packageName,
                    final @ApiParam( allowableValues = CROSS ) @PathParam( "version" ) String version,
                    final @PathParam( "tarball" ) String tarball, @Context final UriInfo uriInfo,
                    @Context final HttpServletRequest request )
    {
        // map /{package}/-/{package}-{version}.tgz to /{package}/{version}/package.tgz
        String v = CROSS;
        try
        {
            v = tarball.substring( packageName.length() + CROSS.length(), tarball.length() - 4 );
        }
        catch ( Exception e )
        {
            logger.error( "Version transform error for tarball: %s", tarball );
        }

        final String path = packageName + PATH_SPLITER + v + PATH_SPLITER + PACAGE_TGZ;

        final String baseUri = uriInfo.getBaseUriBuilder().path( NPM_CONTENT_REST_BASE_PATH ).build().toString();
        return handler.doGet( NPM_PKG_KEY, type, name, path, baseUri, request, new EventMetadata() );
    }

    @Override
    @ApiOperation( "Retrieve root listing under the given artifact store (type/name)." )
    @ApiResponses( { @ApiResponse( code = 200, response = String.class, message = "Rendered root content listing" ),
                    @ApiResponse( code = 200, response = StreamingOutput.class, message = "Content stream" ), } )
    @GET
    @Path( "/" )
    public Response doGet(
                    final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                    final @ApiParam( required = true ) @PathParam( "name" ) String name, @Context final UriInfo uriInfo,
                    @Context final HttpServletRequest request )
    {
        final String baseUri = uriInfo.getBaseUriBuilder().path( NPM_CONTENT_REST_BASE_PATH ).build().toString();

        return handler.doGet( NPM_PKG_KEY, type, name, "", baseUri, request, new EventMetadata() );
    }

}
