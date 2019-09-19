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
package org.commonjava.indy.core.bind.jaxrs.stats;

import java.util.Date;
import java.util.Map;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.bind.jaxrs.IndyDeployment;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.REST;
import org.commonjava.indy.bind.jaxrs.util.ResponseHelper;
import org.commonjava.indy.core.ctl.StatsController;
import org.commonjava.indy.model.core.PackageTypes;
import org.commonjava.indy.model.core.dto.EndpointViewListing;
import org.commonjava.indy.model.spi.AddOnListing;
import org.commonjava.indy.stats.IndyVersioning;
import org.commonjava.indy.util.ApplicationContent;
import org.commonjava.indy.util.UriFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@Api( description = "Various read-only operations for retrieving information about the system.", value = "Generic Infrastructure Queries (UI Support)" )
@Path( "/api/stats" )
@REST
public class StatsHandler
    implements IndyResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StatsController statsController;

    @Inject
    private UriFormatter uriFormatter;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private ResponseHelper responseHelper;

    @ApiOperation( "Retrieve JSON describing the add-ons that are available on the system" )
    @ApiResponse( code = 200, response = AddOnListing.class, message = "The description object" )
    @Path( "/addons/active" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response getAddonList()
    {
        return responseHelper.formatOkResponseWithJsonEntity( statsController.getActiveAddOns() );
    }

    @ApiOperation( "Aggregate javascript content for all add-ons and format as a single Javascript stream (this gives the UI a static URL to load add-on logic)" )
    @ApiResponse( code = 200, message = "The add-on Javascript wrapped as a JSON object" )
    @Path( "/addons/active.js" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response getAddonInjectionJavascript()
    {
        Response response;
        try
        {
            response =
                    responseHelper.formatOkResponseWithEntity( statsController.getActiveAddOnsJavascript(),
                                            ApplicationContent.application_json );
        }
        catch ( final IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to format active-addons javascript: %s", responseHelper.formatEntity( e ) ), e );
            response = responseHelper.formatResponse( e );
        }
        return response;
    }

    @ApiOperation( "Retrieve versioning information about this Indy instance" )
    @ApiResponse( code = 200, response = IndyVersioning.class, message = "The version metadata" )
    @Path( "/version-info" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response getIndyVersion()
    {
        return responseHelper.formatOkResponseWithJsonEntity( statsController.getVersionInfo() );
    }

    @ApiOperation( "Retrieve a mapping of the package type names to descriptors (eg. maven, npm, generic-http, etc) available on the system." )
    @ApiResponse( code = 200, response = Map.class, message = "The package type listing of packageType => details" )
    @Path( "/package-type/map" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response getPackageTypeMap()
    {
        return Response.ok( PackageTypes.getPackageTypeDescriptorMap() ).build();
    }

    @ApiOperation( "Retrieve a list of the package type names (eg. maven, npm, generic-http, etc) available on the system." )
    @ApiResponse( code = 200, response = Map.class, message = "The package type listing" )
    @Path( "/package-type/keys" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response getPackageTypeNames()
    {
        return Response.ok( new TreeSet<>( PackageTypes.getPackageTypes() ) ).build();
    }

    @ApiOperation( "Retrieve a listing of the artifact stores available on the system. This is especially useful for setting up a network of Indy instances that reference one another" )
    @ApiResponse( code = 200, response = EndpointViewListing.class, message = "The artifact store listing" )
    @Path( "/all-endpoints" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response getAllEndpoints( @Context final UriInfo uriInfo )
    {
        Response response;
        try
        {
            final String baseUri = uriInfo.getBaseUriBuilder()
                                          .path( IndyDeployment.API_PREFIX )
                                          .build()
                                          .toString();

            final EndpointViewListing listing = statsController.getEndpointsListing( baseUri, uriFormatter );
            response = responseHelper.formatOkResponseWithJsonEntity( listing );

            logger.info( "\n\n\n\n\n\n{} Sent all-endpoints:\n\n{}\n\n\n\n\n\n\n", new Date(), listing );
        }
        catch ( final IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to retrieve endpoint listing: %s", responseHelper.formatEntity( e ) ), e );
            response = responseHelper.formatResponse( e );
        }
        return response;
    }

}
