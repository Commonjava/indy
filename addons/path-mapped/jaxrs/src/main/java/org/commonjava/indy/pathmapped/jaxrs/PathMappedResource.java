/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.pathmapped.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.SecurityManager;
import org.commonjava.indy.bind.jaxrs.util.REST;
import org.commonjava.indy.bind.jaxrs.util.ResponseHelper;
import org.commonjava.indy.pathmapped.common.PathMappedController;
import org.commonjava.indy.pathmapped.model.PathMappedDeleteResult;
import org.commonjava.indy.pathmapped.model.PathMappedListResult;
import org.commonjava.indy.util.ApplicationHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import java.io.InputStream;

import static org.commonjava.indy.util.ApplicationContent.application_json;
import static org.commonjava.storage.pathmapped.util.PathMapUtils.ROOT_DIR;

@Api( value = "Path-mapped storage maintenance operation", description = "Repair path-mapped storage problems." )
@Path( "/api/admin/pathmapped" )
@REST
public class PathMappedResource
                implements IndyResources
{
    private static final String BROWSE_BASE = "/browse/{packageType}/{type: (hosted|group|remote)}/{name}";

    private static final String CONCRETE_CONTENT_PATH = "/content/{packageType}/{type: (hosted|group|remote)}/{name}/{path: (.*)}";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ObjectMapper mapper;

    @Inject
    private SecurityManager securityManager;

    @Inject
    private PathMappedController controller;

    @Inject
    private ResponseHelper responseHelper;

    @ApiOperation( "List root." )
    @ApiResponse( code = 200, message = "Operation finished.", response = PathMappedListResult.class )
    @GET
    @Path( BROWSE_BASE )
    @Produces( { application_json } )
    public PathMappedListResult listRoot( final @ApiParam( required = true ) @PathParam( "packageType" ) String packageType,
                                      final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                                      final @ApiParam( required = true ) @PathParam( "name" ) String name,
                                      final @QueryParam( "recursive" ) boolean recursive,
                                      final @QueryParam( "type" ) String fileType,
                                      final @QueryParam( "limit" ) int limit,
                                      final @Context HttpServletRequest request,
                                      final @Context SecurityContext securityContext )
    {
        logger.debug( "List, packageType:{}, type:{}, name:{}, recursive:{}", packageType, type, name, recursive );
        return controller.list( packageType, type, name, ROOT_DIR, recursive, fileType, limit );
    }

    @ApiOperation( "List specified path." )
    @ApiResponse( code = 200, message = "Operation finished.", response = PathMappedListResult.class )
    @GET
    @Path( BROWSE_BASE + "/{path: (.*)}" )
    @Produces( { application_json } )
    public PathMappedListResult list( final @ApiParam( required = true ) @PathParam( "packageType" ) String packageType,
                                      final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                                      final @ApiParam( required = true ) @PathParam( "name" ) String name,
                                      final @PathParam( "path" ) String path,
                                      final @QueryParam( "recursive" ) boolean recursive,
                                      final @QueryParam( "type" ) String fileType,
                                      final @QueryParam( "limit" ) int limit,
                                      final @Context HttpServletRequest request,
                                      final @Context SecurityContext securityContext )
    {
        logger.debug( "List, packageType:{}, type:{}, name:{}, path:{}, recursive:{}", packageType, type, name, path,
                      recursive );
        return controller.list( packageType, type, name, path, recursive, fileType, limit );
    }

    @ApiOperation( "Get specified path." )
    @ApiResponse( code = 200, message = "Operation finished." )
    @GET
    @Path( CONCRETE_CONTENT_PATH )
    public Response get( final @PathParam( "packageType" ) String packageType,
                            final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                            final @ApiParam( required = true ) @PathParam( "name" ) String name,
                            final @PathParam( "path" ) String path,
                            final @Context HttpServletRequest request,
                            final @Context SecurityContext securityContext )
    {
        try ( InputStream inputStream = controller.get( packageType, type, name, path ) )
        {
            logger.debug( "Get, packageType:{}, type:{}, name:{}, path:{}", packageType, type, name, path );
            return Response.ok( inputStream )
                           .header( ApplicationHeader.content_disposition.key(),
                                    "attachment" )
                           .build();
        }
        catch ( Exception e )
        {
            logger.error( e.getMessage(), e );
            responseHelper.throwError( e );
        }
        return null;
    }

    @ApiOperation( "Delete specified path." )
    @ApiResponse( code = 200, message = "Operation finished.", response = PathMappedDeleteResult.class )
    @DELETE
    @Path( CONCRETE_CONTENT_PATH )
    @Produces( { application_json } )
    public PathMappedDeleteResult delete( final @PathParam( "packageType" ) String packageType,
                                          final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                                          final @ApiParam( required = true ) @PathParam( "name" ) String name,
                                          final @PathParam( "path" ) String path,
                                          final @Context HttpServletRequest request,
                                          final @Context SecurityContext securityContext )
    {
        try
        {
            logger.debug( "Delete, packageType:{}, type:{}, name:{}, path:{}", packageType, type, name, path );
            return controller.delete( packageType, type, name, path );
        }
        catch ( Exception e )
        {
            logger.error( e.getMessage(), e );
            responseHelper.throwError( e );
        }
        return null;
    }
}
