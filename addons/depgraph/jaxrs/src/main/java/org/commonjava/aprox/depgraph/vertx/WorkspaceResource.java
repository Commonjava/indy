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
package org.commonjava.aprox.depgraph.vertx;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatCreatedResponse;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_wsid;
import static org.commonjava.aprox.util.ApplicationContent.application_json;
import static org.commonjava.vertx.vabr.types.BuiltInParam._classContextUrl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.util.JaxRsUriFormatter;
import org.commonjava.aprox.depgraph.rest.WorkspaceController;
import org.commonjava.aprox.model.core.dto.CreationDTO;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.util.Respond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/depgraph/ws" )
@ApplicationScoped
public class WorkspaceResource
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private WorkspaceController controller;

    @Route( path = "/:wsid", method = Method.DELETE )
    public void delete( final HttpServerRequest request )
    {
        final String id = request.params()
                                 .get( p_wsid.key() );
        try
        {
            controller.delete( id );
            Respond.to( request )
                   .ok()
                   .send();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( path = "/:wsid", method = Method.PUT, contentType = application_json )
    public void createNamed( final HttpServerRequest request )
    {
        final String id = request.params()
                                 .get( p_wsid.key() );
        try
        {
            final CreationDTO dto =
                controller.createNamed( id, request.params()
                                                   .get( _classContextUrl.key() ), new JaxRsUriFormatter() );
            if ( dto != null )
            {
                formatCreatedResponse( request, dto );
            }
            else
            {
                Respond.to( request )
                       .notModified()
                       .send();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( path = "/new", method = Method.POST, contentType = application_json )
    public void create( final HttpServerRequest request )
    {
        try
        {
            final String baseUri = request.params()
                                          .get( _classContextUrl.key() );

            final CreationDTO dto = controller.create( baseUri, new JaxRsUriFormatter() );
            if ( dto != null )
            {
                formatCreatedResponse( request, dto );
            }
            else
            {
                Respond.to( request )
                       .notModified()
                       .send();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( path = "/new/from", method = Method.POST, contentType = application_json )
    public void createFrom( final Buffer body, final HttpServerRequest request )
    {
        try
        {
            // FIXME Figure out the character encoding!
            final CreationDTO dto =
                controller.createFrom( request.params()
                                              .get( _classContextUrl.key() ), new JaxRsUriFormatter(),
                                       body.getString( 0, body.length() ) );
            if ( dto != null )
            {
                formatCreatedResponse( request, dto );
            }
            else
            {
                Respond.to( request )
                       .notModified()
                       .send();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( path = "/:wsid", contentType = application_json )
    public void get( final HttpServerRequest request )
    {
        final String id = request.params()
                                 .get( p_wsid.key() );
        try
        {
            final String json = controller.get( id );
            if ( json == null )
            {
                Respond.to( request )
                       .notFound()
                       .send();
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

    @Route( contentType = application_json )
    public void list( final HttpServerRequest request )
    {
        String json;
        try
        {
            json = controller.list();
        }
        catch ( final AproxWorkflowException e )
        {
            Respond.to( request )
                   .serverError( e, true )
                   .send();
            return;
        }

        if ( json == null )
        {
            Respond.to( request )
                   .notFound()
                   .send();
        }
        else
        {
            formatOkResponseWithJsonEntity( request, json );
        }
    }
}
