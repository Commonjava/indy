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
import static org.commonjava.vertx.vabr.types.BuiltInParam._classContextUrl;

import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.core.rest.StatsController;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.aprox.util.StringFormat;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.web.json.ser.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/stats" )
public class BasicStatsResource
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @AproxData
    private JsonSerializer serializer;

    @Inject
    private StatsController statsController;

    @Inject
    private UriFormatter uriFormatter;

    @Routes( { @Route( path = "/version-info", method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void getAProxVersion( final Buffer buffer, final HttpServerRequest request )
    {
        formatOkResponseWithJsonEntity( request, serializer.toString( statsController.getVersionInfo() ) );
    }

    @Routes( { @Route( path = "/all-endpoints", method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void getAllEndpoints( final Buffer buffer, final HttpServerRequest request )
    {
        try
        {
            final String baseUri = request.params()
                                          .get( _classContextUrl.key() );

            final String json = serializer.toString( statsController.getEndpointsListing( baseUri, uriFormatter ) );

            formatOkResponseWithJsonEntity( request, json );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "{}", e, new StringFormat( "Failed to retrieve endpoint listing: {}", formatEntity( e ) ) );
            formatResponse( e, request );
        }
    }

}
