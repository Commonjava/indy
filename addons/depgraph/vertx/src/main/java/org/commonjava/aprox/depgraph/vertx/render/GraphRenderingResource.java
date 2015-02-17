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
package org.commonjava.aprox.depgraph.vertx.render;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_artifactId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_groupId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_version;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParamUtils.getWorkspaceId;
import static org.commonjava.aprox.model.util.HttpUtils.parseQueryMap;
import static org.commonjava.aprox.util.ApplicationContent.application_xml;
import static org.commonjava.aprox.util.ApplicationContent.text_plain;
import static org.commonjava.vertx.vabr.types.Method.POST;

import java.io.File;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.vertx.util.ResponseUtils;
import org.commonjava.aprox.depgraph.rest.RenderingController;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.util.Respond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/depgraph/render" )
@ApplicationScoped
public class GraphRenderingResource
    implements RequestHandler
{

    private static final String TYPE_GRAPHVIZ = "text/x-graphviz";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private RenderingController controller;

    @Route( path = "/bom/:groupId/:artifactId/:version", method = POST, contentType = application_xml )
    @Deprecated
    public void bomFor( final Buffer body, final HttpServerRequest request )
    {
        ResponseUtils.markDeprecated( request, "/depgraph/render/bom" );

        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );

        try
        {
            final String out =
                controller.bomFor( gid, aid, ver, getWorkspaceId( request ), parseQueryMap( request.query() ),
                                   body.getString( 0, body.length() ) );
            if ( out == null )
            {
                Respond.to( request )
                       .notFound()
                       .send();
            }
            else
            {
                formatOkResponseWithEntity( request, out, application_xml );
            }

        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( path = "/bom", method = POST, contentType = application_xml )
    public void bomForDTO( final Buffer body, final HttpServerRequest request )
    {
        try
        {
            final String out = controller.bomFor( body.getString( 0, body.length() ) );
            if ( out == null )
            {
                Respond.to( request )
                       .notFound()
                       .send();
            }
            else
            {
                formatOkResponseWithEntity( request, out, application_xml );
            }

        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( path = "/dotfile/:groupId/:artifactId/:version", contentType = TYPE_GRAPHVIZ )
    public void dotfile( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );

        try
        {
            final String out =
                controller.dotfile( gid, aid, ver, getWorkspaceId( request ), parseQueryMap( request.query() ) );
            if ( out == null )
            {
                Respond.to( request )
                       .notFound()
                       .send();
            }
            else
            {
                formatOkResponseWithEntity( request, out, TYPE_GRAPHVIZ );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( path = "/tree", method = POST, contentType = text_plain )
    public void tree( final Buffer body, final HttpServerRequest request )
    {
        try
        {
            final File out = controller.tree( body.getString( 0, body.length() ) );
            if ( out == null )
            {
                Respond.to( request )
                       .notFound()
                       .send();
            }
            else
            {
                request.response()
                       .putHeader( ApplicationHeader.content_type.key(), ApplicationContent.text_plain );

                request.response()
                       .sendFile( out.getAbsolutePath() )
                       .close();
                //                formatOkResponseWithEntity( request, out, text_plain );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }
}
