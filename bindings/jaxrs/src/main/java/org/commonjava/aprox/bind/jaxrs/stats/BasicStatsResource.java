/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.bind.jaxrs.stats;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.bind.jaxrs.util.JaxRsUriFormatter;
import org.commonjava.aprox.core.rest.StatsController;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.web.json.ser.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/stats" )
@javax.enterprise.context.ApplicationScoped
public class BasicStatsResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @AproxData
    private JsonSerializer serializer;

    @Inject
    private StatsController statsController;

    @Context
    private UriInfo uriInfo;

    @GET
    @Path( "/addons/active" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getAddonList()
    {
        return Response.ok( serializer.toString( statsController.getActiveAddOns() ) )
                       .build();
    }

    @GET
    @Path( "/addons/active.js" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getAddonInjectionJavascript()
    {
        return Response.ok( serializer.toString( statsController.getActiveAddOns() ) )
                       .build();
    }

    @GET
    @Path( "/version-info" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getAProxVersion()
    {
        return Response.ok( serializer.toString( statsController.getVersionInfo() ) )
                       .build();
    }

    @GET
    @Path( "/all-endpoints" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getAllEndpoints()
    {
        try
        {
            final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();

            final String json =
                serializer.toString( statsController.getEndpointsListing( uriBuilder.path( getClass() )
                                                                                    .build()
                                                                                    .toString(),
                                                                          new JaxRsUriFormatter( uriInfo ) ) );

            return Response.ok( json )
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to retrieve endpoint listing: %s",
                                         AproxExceptionUtils.formatEntity( e ) ), e );
            return AproxExceptionUtils.formatResponse( e );
        }

    }

}
