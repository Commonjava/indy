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
package org.commonjava.aprox.core.bind.vertx.stats;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatEntity;
import static org.commonjava.vertx.vabr.types.BuiltInParam._classContextUrl;

import java.util.Date;

import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.core.ctl.StatsController;
import org.commonjava.aprox.model.core.dto.EndpointViewListing;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.util.Respond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Handles( prefix = "/stats" )
public class StatsHandler
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StatsController statsController;

    @Inject
    private UriFormatter uriFormatter;

    @Inject
    private ObjectMapper objectMapper;

    @Routes( { @Route( path = "/addons/active", method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void getAddonList( final Buffer buffer, final HttpServerRequest request )
    {
        try
        {
            Respond.to( request )
                   .jsonEntity( statsController.getActiveAddOns(), objectMapper )
                   .send();
        }
        catch ( final JsonProcessingException e )
        {
            Respond.to( request )
                   .serverError( e, "Failed to serialize to JSON.", true )
                   .send();
        }
    }

    @Routes( { @Route( path = "/addons/active.js", method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void getAddonInjectionJavascript( final Buffer buffer, final HttpServerRequest request )
    {
        try
        {
            Respond.to( request )
                   .ok()
                   .entity( statsController.getActiveAddOnsJavascript() )
                   .send();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to format active-addons javascript: %s", formatEntity( e ) ), e );
            Respond.to( request )
                   .serverError( e, true );
        }
    }

    @Routes( { @Route( path = "/version-info", method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void getAProxVersion( final Buffer buffer, final HttpServerRequest request )
    {
        try
        {
            Respond.to( request )
                   .jsonEntity( statsController.getVersionInfo(), objectMapper )
                   .send();
        }
        catch ( final JsonProcessingException e )
        {
            Respond.to( request )
                   .serverError( e, "Failed to serialize to JSON.", true )
                   .send();
        }
    }

    @Routes( { @Route( path = "/all-endpoints", method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void getAllEndpoints( final Buffer buffer, final HttpServerRequest request )
    {
        try
        {
            final String baseUri = request.params()
                                          .get( _classContextUrl.key() );

            final EndpointViewListing listing = statsController.getEndpointsListing( baseUri, uriFormatter );
            //            final String json = serializer.toString( listing );

            //            formatOkResponseWithJsonEntity( request, json );
            Respond.to( request )
                   .ok()
                   .jsonEntity( listing, objectMapper )
                   .send();

            logger.info( "\n\n\n\n\n\n{} Sent all-endpoints:\n\n{}\n\n\n\n\n\n\n", new Date(), listing );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to retrieve endpoint listing: %s", formatEntity( e ) ), e );
            Respond.to( request )
                   .serverError( e, true )
                   .send();
            //            formatResponse( e, request );
        }
        catch ( final JsonProcessingException e )
        {
            logger.error( String.format( "Failed to format endpoint listing: %s", formatEntity( e ) ), e );
            Respond.to( request )
                   .serverError( e, true )
                   .send();
        }
    }

}
