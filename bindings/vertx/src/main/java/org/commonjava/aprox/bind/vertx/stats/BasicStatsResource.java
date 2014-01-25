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
package org.commonjava.aprox.bind.vertx.stats;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;

import javax.inject.Inject;

import org.commonjava.aprox.core.rest.StatsController;
import org.commonjava.aprox.core.util.UriFormatter;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationContent;
import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.Method;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.web.json.ser.JsonSerializer;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/stats" )
public class BasicStatsResource
    implements RequestHandler
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    @AproxData
    private JsonSerializer serializer;

    @Inject
    private StatsController statsController;

    @Inject
    private UriFormatter uriFormatter;

    @Routes( { @Route( path = "/version-info", method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void getAProxVersion( final HttpServerRequest request )
    {
        formatOkResponseWithJsonEntity( request, serializer.toString( statsController.getVersionInfo() ) );
    }

    @Routes( { @Route( path = "/all-endpoints", method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void getAllEndpoints( final HttpServerRequest request )
    {
        try
        {
            final String json = serializer.toString( statsController.getEndpointsListing( uriFormatter ) );

            formatOkResponseWithJsonEntity( request, json );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to retrieve endpoint listing: %s", e, formatEntity( e ) );
            formatResponse( e, request.response() );
        }
    }

}
