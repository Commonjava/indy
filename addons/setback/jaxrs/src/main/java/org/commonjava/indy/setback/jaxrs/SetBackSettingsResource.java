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
package org.commonjava.indy.setback.jaxrs;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.ResponseUtils;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.setback.rest.SetBackController;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.util.ApplicationContent;

@Api( value="SetBack Backup settings.xml Access", description="Offline-Capable settings.xml Indy Simulation for use in case Indy is inaccessible" )
@Path( "/api/setback" )
public class SetBackSettingsResource
        implements IndyResources
{

    @Inject
    private SetBackController controller;

    @ApiOperation(
            "Return settings.xml that approximates the behavior of the specified Indy group/repository (CAUTION: Indy-hosted content will be unavailable!)" )
    @ApiResponses( { @ApiResponse( code = 400,
                                   message = "Requested repository is hosted on Indy and cannot be simulated via settings.xml" ),
                           @ApiResponse( code = 404,
                                         message = "No such repository or group, or the settings.xml has not been generated." ),
                           @ApiResponse( code = 200, message = "Maven settings.xml content" ) } )
    @Path( "/{type: (remote|group)}/{name}" )
    @GET
    @Produces( ApplicationContent.application_xml )
    public Response get(
            @ApiParam( allowableValues = "hosted,group,remote", required = true ) final @PathParam( "type" ) String t,
            final @PathParam( "name" ) String n )
    {

        final StoreType type = StoreType.get( t );

        if ( StoreType.hosted == type )
        {
            return Response.status( Status.BAD_REQUEST ).build();
        }

        Response response;

        final StoreKey key = new StoreKey( type, n );
        DataFile settingsXml = null;
        try
        {
            settingsXml = controller.getSetBackSettings( key );
        }
        catch ( final IndyWorkflowException e )
        {
            response = ResponseUtils.formatResponse( e );
        }

        if ( settingsXml != null && settingsXml.exists() )
        {
            response = Response.ok( settingsXml ).type( ApplicationContent.application_xml ).build();
        }
        else
        {
            response = Response.status( Status.NOT_FOUND ).build();
        }

        return response;
    }

    @ApiOperation( "DELETE the settings.xml simulation corresponding to the specified Indy group/repository" )
    @ApiResponses( {
                           @ApiResponse( code = 400,
                                         message = "Requested repository is hosted on Indy and cannot be simulated via settings.xml" ),
                           @ApiResponse( code = 404,
                                         message = "No such repository or group, or the settings.xml has not been generated." ),
                           @ApiResponse( code = 204, message = "Deletion succeeded" )
                   } )
    @Path( "/{type: (remote|group)}/{name}" )
    @DELETE
    public Response delete( @ApiParam( allowableValues = "hosted,group,remote", required = true ) final @PathParam( "type" ) String t, final @PathParam( "name" ) String n )
    {
        final StoreType type = StoreType.get( t );

        if ( StoreType.hosted == type )
        {
            return Response.status( Status.BAD_REQUEST ).build();
        }

        Response response;

        final StoreKey key = new StoreKey( type, n );
        try
        {
            final boolean found = controller.deleteSetBackSettings( key );

            if ( found )
            {
                response = Response.status( Status.NO_CONTENT ).build();
            }
            else
            {
                response = Response.status( Status.NOT_FOUND ).build();
            }
        }
        catch ( final IndyWorkflowException e )
        {
            response = ResponseUtils.formatResponse( e );
        }

        return response;
    }

}
