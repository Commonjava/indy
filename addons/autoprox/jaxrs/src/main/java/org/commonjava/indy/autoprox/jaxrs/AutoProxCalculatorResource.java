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
package org.commonjava.indy.autoprox.jaxrs;

import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;

import java.util.Collections;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.autoprox.conf.AutoProxConfig;
import org.commonjava.indy.autoprox.rest.AutoProxCalculatorController;
import org.commonjava.indy.autoprox.rest.dto.AutoProxCalculation;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.util.ApplicationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Api( value="AutoProx Calculator", description="Calculate results of applying existing AutoProx rules without making modifications" )
@Path( "/api/autoprox/eval" )
public class AutoProxCalculatorResource
    implements IndyResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ObjectMapper serializer;

    @Inject
    private AutoProxCalculatorController controller;

    @Inject
    private AutoProxConfig config;

    @ApiOperation( value = "Calculate the effects of referencing a store with the given type and name to determine what AutoProx will auto-create", response = AutoProxCalculation.class )
    @ApiResponse( code=200, message = "Result of calculation" )
    @Path( "/{type: (hosted|group|remote)}/{name}" )
    @GET
    @Produces( ApplicationContent.application_json )
    @Deprecated
    public Response evalDeprecated( final @PathParam("type") String type, final @PathParam( "name" ) String remoteName )
    {
        return eval( MAVEN_PKG_KEY, type, remoteName );
    }

    @ApiOperation( value = "Calculate the effects of referencing a store with the given type and name to determine what AutoProx will auto-create", response = AutoProxCalculation.class )
    @ApiResponse( code=200, message = "Result of calculation" )
    @Path( "/{packageType}/{type: (hosted|group|remote)}/{name}" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response eval( final @PathParam("packageType") String packageType, final @PathParam("type") String type, final @PathParam( "name" ) String remoteName )
    {
        Response response = checkEnabled();
        if ( response != null )
        {
            return response;
        }

        try
        {
            StoreKey key = new StoreKey( packageType, StoreType.get( type ), remoteName );

            final AutoProxCalculation calc =
                    controller.eval( key );

            response =
                formatOkResponseWithJsonEntity( serializer.writeValueAsString( calc == null ? Collections.singletonMap( "error",
                                                                                                          "Nothing was created" )
                                                            : calc ) );
        }
        catch ( final IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to create demo RemoteRepository for: '%s'. Reason: %s", remoteName,
                                         e.getMessage() ), e );
            response = formatResponse( e );
        }
        catch ( final JsonProcessingException e )
        {
            logger.error( String.format( "Failed to create demo RemoteRepository for: '%s'. Reason: %s", remoteName,
                                         e.getMessage() ), e );
            response = formatResponse( e );
        }

        return response;
    }

    private Response checkEnabled()
    {
        if ( !config.isEnabled() )
        {
            return Response.status( Response.Status.BAD_REQUEST ).entity( "Autoprox is disabled." ).build();
        }

        return null;
    }

}
