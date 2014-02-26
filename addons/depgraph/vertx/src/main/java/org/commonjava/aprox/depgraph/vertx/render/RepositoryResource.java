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
package org.commonjava.aprox.depgraph.vertx.render;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.setStatus;
import static org.commonjava.aprox.rest.util.ApplicationContent.application_json;
import static org.commonjava.aprox.rest.util.ApplicationContent.application_zip;
import static org.commonjava.aprox.rest.util.ApplicationContent.text_plain;
import static org.commonjava.vertx.vabr.types.BuiltInParam._classContextUrl;

import javax.inject.Inject;

import org.commonjava.aprox.bind.vertx.util.VertXUriFormatter;
import org.commonjava.aprox.depgraph.rest.RepositoryController;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationContent;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.util.VertXOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/depgraph/repo" )
public class RepositoryResource
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private RepositoryController controller;

    @Route( path = "/urlmap", method = Method.POST, contentType = application_json )
    public void getUrlMap( final Buffer body, final HttpServerRequest request )
    {
        try
        {
            final String baseUri = request.params()
                                          .get( _classContextUrl.key() );

            final String json = controller.getUrlMap( body.getString( 0, body.length() ), baseUri, new VertXUriFormatter() );

            if ( json == null )
            {
                setStatus( ApplicationStatus.NO_CONTENT, request );
            }
            else
            {
                formatOkResponseWithJsonEntity( request, json );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( path = "/downlog", method = Method.POST, contentType = text_plain )
    public void getDownloadLog( final Buffer body, final HttpServerRequest request )
    {
        try
        {
            final String baseUri = request.params()
                                          .get( _classContextUrl.key() );

            final String downlog = controller.getDownloadLog( body.getString( 0, body.length() ), baseUri, new VertXUriFormatter() );
            if ( downlog == null )
            {
                setStatus( ApplicationStatus.NO_CONTENT, request );
            }
            else
            {
                formatOkResponseWithEntity( request, downlog, ApplicationContent.text_plain );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( path = "/zip", method = Method.POST, contentType = application_zip )
    public void getZipRepository( final Buffer body, final HttpServerRequest request )
    {
        try
        {
            controller.getZipRepository( body.getString( 0, body.length() ), new VertXOutputStream( request.response() ) );
            setStatus( ApplicationStatus.OK, request );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }
}
