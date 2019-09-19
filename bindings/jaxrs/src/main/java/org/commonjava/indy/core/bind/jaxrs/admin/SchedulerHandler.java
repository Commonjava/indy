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
package org.commonjava.indy.core.bind.jaxrs.admin;

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
import org.commonjava.indy.core.ctl.SchedulerController;
import org.commonjava.indy.core.expire.Expiration;
import org.commonjava.indy.core.expire.ExpirationSet;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.util.ApplicationContent;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import java.nio.file.Paths;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;

@Api( value = "Schedules and Expirations",
      description = "Retrieve and manipulate scheduled expirations for various parts of Indy" )
@Path( "/api/admin/schedule" )
@Produces( ApplicationContent.application_json )
@REST
public class SchedulerHandler
        implements IndyResources
{
    @Inject
    private SchedulerController controller;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private ResponseHelper responseHelper;

    @ApiOperation( "[Deprecated] Retrieve the expiration information related to re-enablement of a repository" )
    @ApiResponses( { @ApiResponse( code = 200, message = "Expiration information retrieved successfully.", response = Expiration.class ),
                     @ApiResponse( code = 400, message = "Store is manually disabled (doesn't automatically re-enable), or isn't disabled." ) } )
    @Path( "store/{type: (hosted|group|remote)}/{name}/disable-timeout" )
    @GET
    @Deprecated
    public Response deprecatedGetStoreDisableTimeout( @ApiParam( allowableValues = "hosted,group,remote", required=true ) @PathParam( "type" ) String storeType,
                                              @ApiParam( required=true ) @PathParam( "name" ) String storeName )
    {
        StoreType type = StoreType.get( storeType );

        String altPath = Paths.get( "/api/admin/schedule", MAVEN_PKG_KEY, type.singularEndpointName(), storeName ).toString();
        StoreKey storeKey = new StoreKey( type, storeName );
        Expiration timeout = null;
        try
        {
            timeout = controller.getStoreDisableTimeout( storeKey );
            if ( timeout == null )
            {
                throw new WebApplicationException( Response.Status.NOT_FOUND );
            }

            return responseHelper.formatOkResponseWithJsonEntity( timeout,
                                                                  rb -> responseHelper.markDeprecated( rb, altPath ) );
        }
        catch ( IndyWorkflowException e )
        {
            responseHelper.throwError( e, rb->responseHelper.markDeprecated( rb, altPath ) );
        }

        return null;
    }

    @ApiOperation( "Retrieve the expiration information related to re-enablement of a repository" )
    @ApiResponses( { @ApiResponse( code = 200, message = "Expiration information retrieved successfully.", response = Expiration.class ),
                           @ApiResponse( code = 400,
                                         message = "Store is manually disabled (doesn't automatically re-enable), or isn't disabled." ) } )
    @Path( "store/{packageType}/{type: (hosted|group|remote)}/{name}/disable-timeout" )
    @GET
    public Expiration getStoreDisableTimeout(
            @ApiParam( value = "Package type (maven, generic-http, npm, etc)", required = true )
            @PathParam( "packageType" ) String packageType,
            @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String storeType,
            @ApiParam( required = true ) @PathParam( "name" ) String storeName )
    {
        StoreKey storeKey = new StoreKey( packageType, StoreType.get( storeType ), storeName );
        Expiration timeout = null;
        try
        {
            timeout = controller.getStoreDisableTimeout( storeKey );
        }
        catch ( IndyWorkflowException e )
        {
            responseHelper.throwError( e );
        }

        if ( timeout == null )
        {
            throw new WebApplicationException( Response.Status.NOT_FOUND );
        }

        return timeout;
    }

    @ApiOperation( "Retrieve the expiration information related to re-enablement of any currently disabled repositories" )
    @ApiResponse( code = 200, message = "List of disabled repository re-enablement timeouts.", response = ExpirationSet.class )
    @Path( "store/all/disable-timeout" )
    @GET
    public ExpirationSet getDisabledStores()
    {
        try
        {
            return controller.getDisabledStores();
        }
        catch ( IndyWorkflowException e )
        {
            responseHelper.throwError( e );
        }

        throw new WebApplicationException( "Impossible Error", 500 );
    }
}
