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

    @Routes( { @Route( path = "/addons/active", method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void getAddonList( final Buffer buffer, final HttpServerRequest request )
    {
        formatOkResponseWithJsonEntity( request, serializer.toString( statsController.getActiveAddOns() ) );
    }

    @Routes( { @Route( path = "/addons/active.js", method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void getAddonInjectionJavascript( final Buffer buffer, final HttpServerRequest request )
    {
        formatOkResponseWithJsonEntity( request,
                                        "var addons = " + serializer.toString( statsController.getActiveAddOns() )
                                            + ";" );
    }

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
            logger.error( String.format( "Failed to retrieve endpoint listing: %s", formatEntity( e ) ), e );
            formatResponse( e, request );
        }
    }

}
