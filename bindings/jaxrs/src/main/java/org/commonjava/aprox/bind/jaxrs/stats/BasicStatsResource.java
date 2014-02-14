/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/stats" )
@javax.enterprise.context.ApplicationScoped
public class BasicStatsResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    @AproxData
    private JsonSerializer serializer;

    @Inject
    private StatsController statsController;

    @Context
    private UriInfo uriInfo;

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
                                                                                    .toString(), new JaxRsUriFormatter( uriBuilder ) ) );

            return Response.ok( json )
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to retrieve endpoint listing: %s", e, AproxExceptionUtils.formatEntity( e ) );
            return AproxExceptionUtils.formatResponse( e );
        }

    }

}
