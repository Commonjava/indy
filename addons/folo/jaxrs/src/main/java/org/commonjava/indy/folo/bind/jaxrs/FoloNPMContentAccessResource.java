/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.folo.bind.jaxrs;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.commonjava.indy.bind.jaxrs.IndyDeployment;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.REST;
import org.commonjava.indy.core.bind.jaxrs.util.RequestUtils;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.indy.pkg.PackageTypeConstants;
import org.commonjava.indy.pkg.npm.inject.NPMContentHandler;
import org.commonjava.indy.pkg.npm.jaxrs.NPMContentAccessHandler;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
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
import java.nio.file.Paths;

import static org.commonjava.indy.IndyContentConstants.CHECK_CACHE_ONLY;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.CONTENT_TRACKING_ID;
import static org.commonjava.indy.folo.ctl.FoloConstants.ACCESS_CHANNEL;
import static org.commonjava.indy.folo.ctl.FoloConstants.TRACKING_KEY;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_NPM;
import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.commonjava.maven.galley.spi.cache.CacheProvider.STORE_HTTP_HEADERS;

@Api( value = "FOLO Tracked Content Access and Storage For NPM related artifacts. Tracks retrieval and management of file/artifact content." )
@Path( "/api/folo/track/{id}/npm/{type: (hosted|group|remote)}/{name}" )
@REST
public class FoloNPMContentAccessResource
        implements IndyResources
{

    private static final String BASE_PATH = IndyDeployment.API_PREFIX + "/folo/track";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String PACKAGE_JSON = "/package.json";

    @Inject
    @NPMContentHandler
    private NPMContentAccessHandler handler;

    public FoloNPMContentAccessResource()
    {
    }

    public FoloNPMContentAccessResource( final NPMContentAccessHandler handler )
    {
        this.handler = handler;
    }

    @ApiOperation( "Store and track NPM file/artifact content under the given artifact store (type/name) and path." )
    @ApiResponses( { @ApiResponse( code = 201, message = "Content was stored successfully" ), @ApiResponse( code = 400,
                                                                                                            message = "No appropriate storage location was found in the specified store (this store, or a member if a group is specified)." ) } )
    @PUT
    @Path( "/{packageName}" )
    public Response doCreate( @ApiParam( "User-assigned tracking session key" ) @PathParam( "id" ) final String id,
                              @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" )
                              final String type, @PathParam( "name" ) final String name,
                              @PathParam( "packageName" ) final String packageName,
                              @Context final HttpServletRequest request, @Context final UriInfo uriInfo )
    {
        final TrackingKey tk = new TrackingKey( id );

        EventMetadata metadata = new EventMetadata().set( TRACKING_KEY, tk )
                                                    .set( ACCESS_CHANNEL, AccessChannel.NATIVE )
                                                    .set( STORE_HTTP_HEADERS,
                                                          RequestUtils.extractRequestHeadersToMap( request ) );

        Class cls = FoloNPMContentAccessResource.class;
        return handler.doCreate( NPM_PKG_KEY, type, name, packageName, request, metadata,
                                 () -> uriInfo.getBaseUriBuilder()
                                              .path( cls )
                                              .path( packageName )
                                              .build( id, NPM_PKG_KEY, type, name ) );
    }

    @ApiOperation(
            "Store NPM artifact content under the given artifact store (type/name), packageName and versionTarball (/version or /-/tarball)." )
    @ApiResponses( { @ApiResponse( code = 201, message = "Content was stored successfully" ), @ApiResponse( code = 400,
                                                                                                            message = "No appropriate storage location was found in the specified store (this store, or a member if a group is specified)." ) } )
    @PUT
    @Path( "/{packageName}/{versionTarball: (.*)}" )
    public Response doCreate( @ApiParam( "User-assigned tracking session key" ) @PathParam( "id" ) final String id,
                              final @ApiParam( allowableValues = "hosted,group,remote", required = true )
                              @PathParam( "type" ) String type,
                              final @ApiParam( required = true ) @PathParam( "name" ) String name,
                              final @PathParam( "packageName" ) String packageName,
                              final @PathParam( "versionTarball" ) String versionTarball,
                              final @Context UriInfo uriInfo, final @Context HttpServletRequest request )
    {
        final TrackingKey tk = new TrackingKey( id );
        EventMetadata metadata =
                new EventMetadata().set( TRACKING_KEY, tk ).set( ACCESS_CHANNEL, AccessChannel.NATIVE );
        final String path = Paths.get( packageName, versionTarball ).toString();
        Class cls = FoloNPMContentAccessResource.class;
        return handler.doCreate( NPM_PKG_KEY, type, name, path, request, metadata, () -> uriInfo.getBaseUriBuilder()
                                                                                                .path( cls )
                                                                                                .path( path )
                                                                                                .build( NPM_PKG_KEY,
                                                                                                        type, name ) );
    }

    @ApiOperation( "Store and track file/artifact content under the given artifact store (type/name) and path." )
    @ApiResponses( { @ApiResponse( code = 404, message = "Content is not available" ), @ApiResponse( code = 200,
                                                                                                     message = "Header metadata for content (or rendered listing when path ends with '/index.html' or '/'" ), } )
    @HEAD
    @Path( "/{packageName}" )
    public Response doHead( @ApiParam( "User-assigned tracking session key" ) @PathParam( "id" ) final String id,
                            final @ApiParam( allowableValues = "hosted,group,remote", required = true )
                            @PathParam( "type" ) String type,
                            final @ApiParam( required = true ) @PathParam( "name" ) String name,
                            final @PathParam( "packageName" ) String packageName,
                            final @QueryParam( CHECK_CACHE_ONLY ) Boolean cacheOnly, final @Context UriInfo uriInfo,
                            final @Context HttpServletRequest request )
    {
        final TrackingKey tk = new TrackingKey( id );

        final String baseUri = getBasePath( uriInfo, id );

        EventMetadata metadata = new EventMetadata().set( TRACKING_KEY, tk )
                                                    .set( ACCESS_CHANNEL, AccessChannel.NATIVE );

        MDC.put( CONTENT_TRACKING_ID, id );

        return handler.doHead( NPM_PKG_KEY, type, name, packageName, cacheOnly, baseUri, request, metadata );
    }

    @ApiOperation( "Store and track file/artifact content under the given artifact store (type/name) and path." )
    @ApiResponses( { @ApiResponse( code = 404, message = "Content is not available" ), @ApiResponse( code = 200,
                                                                                                     message = "Header metadata for content (or rendered listing when path ends with '/index.html' or '/'" ), } )
    @HEAD
    @Path( "/{packageName}/{versionTarball: (.*)}" )
    public Response doHead( @ApiParam( "User-assigned tracking session key" ) @PathParam( "id" ) final String id,
                            @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" )
                            final String type, @PathParam( "name" ) final String name,
                            final @PathParam( "packageName" ) String packageName,
                            final @PathParam( "versionTarball" ) String versionTarball,
                            final @QueryParam( CHECK_CACHE_ONLY ) Boolean cacheOnly,
                            @Context final HttpServletRequest request, @Context final UriInfo uriInfo )
    {
        final TrackingKey tk = new TrackingKey( id );

        EventMetadata metadata =
                new EventMetadata().set( TRACKING_KEY, tk ).set( ACCESS_CHANNEL, AccessChannel.NATIVE );

        MDC.put( CONTENT_TRACKING_ID, id );

        final String baseUri =  getBasePath( uriInfo, id );
        final String path = Paths.get( packageName, versionTarball ).toString();

        return handler.doHead( NPM_PKG_KEY, type, name, path, cacheOnly, baseUri, request, metadata );
    }

    @ApiOperation( "Retrieve and track NPM file/artifact content under the given artifact store (type/name) and path." )
    @ApiResponses( { @ApiResponse( code = 404, message = "Content is not available" ),
                           @ApiResponse( code = 200, response = String.class,
                                         message = "Rendered content listing (when path ends with '/index.html' or '/')" ),
                           @ApiResponse( code = 200, response = StreamingOutput.class, message = "Content stream" ), } )
    @GET
    @Path( "/{packageName}" )
    public Response doGet( @ApiParam( "User-assigned tracking session key" ) @PathParam( "id" ) final String id,
                           @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" )
                           final String type, @PathParam( "name" ) final String name,
                           final @PathParam( "packageName" ) String packageName,
                           @Context final HttpServletRequest request, @Context final UriInfo uriInfo )
    {
        final TrackingKey tk = new TrackingKey( id );
        final String baseUri = getBasePath( uriInfo, id );

        EventMetadata metadata = new EventMetadata().set( TRACKING_KEY, tk )
                                                    .set( ACCESS_CHANNEL, AccessChannel.NATIVE );

        MDC.put( CONTENT_TRACKING_ID, id );

        return handler.doGet( NPM_PKG_KEY, type, name, packageName, baseUri, request, metadata );
    }

    @ApiOperation( "Retrieve and track NPM file/artifact content under the given artifact store (type/name) and path." )
    @ApiResponses( { @ApiResponse( code = 404, message = "Content is not available" ),
                           @ApiResponse( code = 200, response = String.class,
                                         message = "Rendered content listing (when path ends with '/index.html' or '/')" ),
                           @ApiResponse( code = 200, response = StreamingOutput.class, message = "Content stream" ), } )
    @GET
    @Path( "/{packageName}/{versionTarball: (.*)}" )
    public Response doGet( @ApiParam( "User-assigned tracking session key" ) @PathParam( "id" ) final String id,
                           @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" )
                           final String type, @PathParam( "name" ) final String name,
                           final @PathParam( "packageName" ) String packageName,
                           final @PathParam( "versionTarball" ) String versionTarball,
                           @Context final HttpServletRequest request, @Context final UriInfo uriInfo )
    {
        final TrackingKey tk = new TrackingKey( id );
        EventMetadata metadata =
                new EventMetadata().set( TRACKING_KEY, tk ).set( ACCESS_CHANNEL, AccessChannel.NATIVE );
        MDC.put( CONTENT_TRACKING_ID, id );

        final String path = Paths.get( packageName, versionTarball ).toString();
        final String baseUri = getBasePath( uriInfo, id );

        return handler.doGet( NPM_PKG_KEY, type, name, path, baseUri, request, metadata );
    }

    private String getBasePath( final UriInfo uriInfo, final String trackId )
    {
        return uriInfo.getBaseUriBuilder().path( BASE_PATH ).path( trackId ).path( PKG_TYPE_NPM ).build().toString();
    }
}
