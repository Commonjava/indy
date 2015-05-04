/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.core.bind.jaxrs.stats;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;

import java.util.Date;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxDeployment;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.core.ctl.StatsController;
import org.commonjava.aprox.model.core.dto.EndpointViewListing;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.aprox.util.UriFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path( "/api/stats" )
public class StatsHandler
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StatsController statsController;

    @Inject
    private UriFormatter uriFormatter;

    @Inject
    private ObjectMapper objectMapper;

    @Path( "/addons/active" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response getAddonList()
    {
        return formatOkResponseWithJsonEntity( statsController.getActiveAddOns(), objectMapper );
    }

    @Path( "/addons/active.js" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response getAddonInjectionJavascript()
    {
        Response response;
        try
        {
            response =
                formatOkResponseWithEntity( statsController.getActiveAddOnsJavascript(),
                                            ApplicationContent.application_json );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to format active-addons javascript: %s", formatEntity( e ) ), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/version-info" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response getAProxVersion()
    {
        return formatOkResponseWithJsonEntity( statsController.getVersionInfo(), objectMapper );
    }

    @Path( "/all-endpoints" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response getAllEndpoints( @Context final UriInfo uriInfo )
    {
        Response response;
        try
        {
            final String baseUri = uriInfo.getBaseUriBuilder()
                                          .path( AproxDeployment.API_PREFIX )
                                          .build()
                                          .toString();

            final EndpointViewListing listing = statsController.getEndpointsListing( baseUri, uriFormatter );
            response = formatOkResponseWithJsonEntity( listing, objectMapper );

            logger.info( "\n\n\n\n\n\n{} Sent all-endpoints:\n\n{}\n\n\n\n\n\n\n", new Date(), listing );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to retrieve endpoint listing: %s", formatEntity( e ) ), e );
            response = formatResponse( e, true );
        }
        return response;
    }

}
