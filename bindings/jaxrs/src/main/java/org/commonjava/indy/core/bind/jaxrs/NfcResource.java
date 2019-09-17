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
package org.commonjava.indy.core.bind.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.REST;
import org.commonjava.indy.bind.jaxrs.util.ResponseHelper;
import org.commonjava.indy.core.ctl.NfcController;
import org.commonjava.indy.core.model.Page;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.dto.NotFoundCacheDTO;
import org.commonjava.indy.model.core.dto.NotFoundCacheInfoDTO;
import org.commonjava.indy.core.model.Pagination;
import org.commonjava.indy.util.ApplicationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.nio.file.Paths;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;

@Api( description = "REST resource that manages the not-found cache", value = "Not-Found Cache" )
@Path( "/api/nfc" )
@REST
public class NfcResource
        implements IndyResources
{

    @Inject
    private NfcController controller;

    @Inject
    private ObjectMapper serializer;

    @Inject
    private ResponseHelper responseHelper;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @ApiOperation( "Clear all not-found cache entries" )
    @DELETE
    public Response clearAll()
    {
        controller.clear();
        return Response.ok().build();
    }

    @Path( "/{type: (hosted|group|remote)}/{name}{path: (/.+)?}" )
    @ApiOperation(
            "[Deprecated] Clear all not-found cache entries for a particular store (or optionally, a subpath within a store)" )
    @DELETE
    @Deprecated
    public Response deprecatedClearStore(
            final @ApiParam( allowableValues = "hosted,group,remote", name = "type", required = true,
                             value = "The type of store" )
            @PathParam( "type" ) String t,
            final @ApiParam( name = "name", required = true, value = "The name of the store" )
            @PathParam( "name" ) String name,
            final @ApiParam( name = "path", required = false, value = "The sub-path to clear" )
            @PathParam( "path" ) String p )
    {

        Response response;
        final StoreType type = StoreType.get( t );

        String altPath = Paths.get( "/api/nfc", MAVEN_PKG_KEY, type.singularEndpointName(), name ).toString();
        final StoreKey key = new StoreKey( type, name );
        try
        {
            if ( isBlank( p ) )
            {
                controller.clear( key );
            }
            else
            {
                controller.clear( key, p );
            }

            response = responseHelper.markDeprecated( Response.ok(), altPath ).build();
        }
        catch ( final IndyWorkflowException e )
        {
            response = responseHelper.formatResponse( e, ( rb ) -> responseHelper.markDeprecated( rb, altPath ) );
        }

        return response;
    }

    @Path( "/{packageType}/{type: (hosted|group|remote)}/{name}{path: (/.+)?}" )
    @ApiOperation(
            "Clear all not-found cache entries for a particular store (or optionally, a subpath within a store)" )
    @DELETE
    public Response clearStore( final @ApiParam( name = "packageType", required = true,
                                                 value = "The type of package (eg. maven, npm, generic-http)" )
                                @PathParam( "packageType" ) String packageType,
                                final @ApiParam( allowableValues = "hosted,group,remote", name = "type",
                                                 required = true, value = "The type of store" )
                                @PathParam( "type" ) String t,
                                final @ApiParam( name = "name", required = true, value = "The name of the store" )
                                @PathParam( "name" ) String name,
                                final @ApiParam( name = "path", required = false, value = "The sub-path to clear" )
                                @PathParam( "path" ) String p )
    {
        Response response;
        final StoreType type = StoreType.get( t );
        try
        {
            final StoreKey key = new StoreKey( packageType, type, name );
            if ( isBlank( p ) )
            {
                controller.clear( key );
            }
            else
            {
                controller.clear( key, p );
            }

            response = Response.ok().build();
        }
        catch ( final IndyWorkflowException e )
        {
            response = responseHelper.formatResponse( e );
        }

        return response;
    }

    @GET
    @ApiOperation( "Retrieve all not-found cache entries currently tracked" )
    @ApiResponses(
            { @ApiResponse( code = 200, response = NotFoundCacheDTO.class, message = "The full not-found cache" ) } )
    @Produces( ApplicationContent.application_json )
    public Response getAll(
                    final @ApiParam( name = "pageIndex", value = "page index" )
                    @QueryParam( "pageIndex" ) Integer pageIndex,
                    final @ApiParam( name = "pageSize", value = "page size" )
                    @QueryParam( "pageSize" ) Integer pageSize )
    {
        NotFoundCacheDTO dto;
        Page page = new Page( pageIndex, pageSize);
        if ( page != null && page.allowPaging() )
        {
            Pagination<NotFoundCacheDTO> nfcPagination = controller.getAllMissing( page );
            dto = nfcPagination.getCurrData();
        }
        else {
            dto = controller.getAllMissing();
        }
        return responseHelper.formatOkResponseWithJsonEntity( dto );
    }

    @GET
    @Path( "/info" )
    @ApiOperation( "Get not-found cache information, e.g., size, etc" )
    @ApiResponses(
                    { @ApiResponse( code = 200, response = NotFoundCacheInfoDTO.class, message = "The info of not-found cache" ) } )
    @Produces( ApplicationContent.application_json )
    public Response getInfo( )
    {
        NotFoundCacheInfoDTO dto = controller.getInfo();
        return responseHelper.formatOkResponseWithJsonEntity( dto );
    }

    @Path( "/{type: (hosted|group|remote)}/{name}" )
    @ApiOperation( "[Deprecated] Retrieve all not-found cache entries currently tracked for a given store" )
    @ApiResponses( { @ApiResponse( code = 200, response = NotFoundCacheDTO.class,
                                   message = "The not-found cache for the specified artifact store" ) } )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response deprecatedGetStore(
                    final @ApiParam( allowableValues = "hosted,group,remote", name = "type", required = true,
                                    value = "The type of store" )
                    @PathParam( "type" ) String t,
                    final @ApiParam( name = "name", value = "The name of the store" )
                    @PathParam( "name" ) String name,
                    final @ApiParam( name = "pageIndex", value = "page index" )
                    @QueryParam( "pageIndex" ) Integer pageIndex,
                    final @ApiParam( name = "pageSize", value = "page size" )
                    @QueryParam( "pageSize" ) Integer pageSize )
    {
        Response response;
        final StoreType type = StoreType.get( t );

        String altPath = Paths.get( "/api/nfc", MAVEN_PKG_KEY, type.singularEndpointName(), name ).toString();
        final StoreKey key = new StoreKey( type, name );
        try
        {
            NotFoundCacheDTO dto;
            Page page = new Page(pageIndex, pageSize);
            if ( page != null && page.allowPaging() )
            {
                Pagination<NotFoundCacheDTO> nfcPagination = controller.getMissing( key, page );
                dto = nfcPagination.getCurrData();
            }
            else
            {
                dto = controller.getMissing( key );
            }
            response = responseHelper.formatOkResponseWithJsonEntity( dto,
                                                                      rb->responseHelper.markDeprecated( rb, altPath ) );
        }
        catch ( final IndyWorkflowException e )
        {
            response = responseHelper.formatResponse( e, ( rb ) -> responseHelper.markDeprecated( rb, altPath ) );
        }

        return response;
    }

    @Path( "/{packageType}/{type: (hosted|group|remote)}/{name}/info" )
    @ApiOperation( "Get not-found cache information, e.g., size, etc" )
    @ApiResponses(
                    { @ApiResponse( code = 200, response = NotFoundCacheInfoDTO.class, message = "The info of not-found cache" ) } )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response getStoreInfo(
                    final @ApiParam( name = "packageType", required = true, value = "type of package (eg. maven, npm, generic-http)" )
                    @PathParam( "packageType" ) String packageType,
                    final @ApiParam( allowableValues = "hosted,group,remote", name = "type", required = true, value = "type of store" )
                    @PathParam( "type" ) String t,
                    final @ApiParam( name = "name", value = "name of the store" )
                    @PathParam( "name" ) String name )
    {
        Response response;
        final StoreType type = StoreType.get( t );
        final StoreKey key = new StoreKey( packageType, type, name );
        try
        {
            NotFoundCacheInfoDTO dto = controller.getInfo( key );
            response = responseHelper.formatOkResponseWithJsonEntity( dto );
        }
        catch ( final IndyWorkflowException e )
        {
            response = responseHelper.formatResponse( e );
        }
        return response;
    }

    @Path( "/{packageType}/{type: (hosted|group|remote)}/{name}" )
    @ApiOperation( "Retrieve all not-found cache entries currently tracked for a given store" )
    @ApiResponses( { @ApiResponse( code = 200, response = NotFoundCacheDTO.class,
                                   message = "The not-found cache for the specified artifact store" ) } )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response getStore(
                    final @ApiParam( name = "packageType", required = true, value = "type of package (eg. maven, npm, generic-http)" )
                    @PathParam( "packageType" ) String packageType,
                    final @ApiParam( allowableValues = "hosted,group,remote", name = "type", required = true, value = "type of store" )
                    @PathParam( "type" ) String t,
                    final @ApiParam( name = "name", value = "name of the store" )
                    @PathParam( "name" ) String name,
                    final @ApiParam( name = "pageIndex", value = "page index" )
                    @QueryParam( "pageIndex" ) Integer pageIndex,
                    final @ApiParam( name = "pageSize", value = "page size" )
                    @QueryParam( "pageSize" ) Integer pageSize )
    {
        Response response;
        final StoreType type = StoreType.get( t );
        final StoreKey key = new StoreKey( packageType, type, name );
        try
        {
            NotFoundCacheDTO dto;
            Page page = new Page(pageIndex, pageSize);
            if ( page != null && page.allowPaging() )
            {
                Pagination<NotFoundCacheDTO> nfcPagination = controller.getMissing( key, page );
                dto = nfcPagination.getCurrData();

            }
            else
            {
                dto = controller.getMissing( key );
            }
            response = responseHelper.formatOkResponseWithJsonEntity( dto );
        }
        catch ( final IndyWorkflowException e )
        {
            response = responseHelper.formatResponse( e );
        }
        return response;
    }
}
