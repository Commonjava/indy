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
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.setStatus;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_artifactId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_groupId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_version;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.q_scope;
import static org.commonjava.aprox.rest.util.ApplicationContent.application_xml;
import static org.commonjava.aprox.rest.util.ApplicationContent.text_plain;
import static org.commonjava.aprox.rest.util.RequestUtils.parseQueryMap;
import static org.commonjava.vertx.vabr.Method.POST;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.bind.vertx.util.VertXInputStream;
import org.commonjava.aprox.depgraph.rest.RenderingController;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/depgraph/render/graph" )
@ApplicationScoped
public class GraphRenderingResource
    implements RequestHandler
{

    private static final String TYPE_GRAPHVIZ = "text/x-graphviz";

    private final Logger logger = new Logger( getClass() );

    @Inject
    private RenderingController controller;

    @Route( path = "/bom/:groupId/:artifactId/:version", method = POST, contentType = application_xml )
    public void bomFor( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );

        try
        {
            final String out = controller.bomFor( gid, aid, ver, parseQueryMap( request.query() ), new VertXInputStream( request ) );
            if ( out == null )
            {
                setStatus( ApplicationStatus.NOT_FOUND, request );
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
            final String out = controller.dotfile( gid, aid, ver, parseQueryMap( request.query() ) );
            if ( out == null )
            {
                setStatus( ApplicationStatus.NOT_FOUND, request );
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

    @Route( path = "/tree/:groupId/:artifactId/:version", contentType = text_plain )
    public void depTree( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );
        final String scope = params.get( q_scope.key() );

        try
        {
            final String out =
                controller.depTree( gid, aid, ver, scope == null ? DependencyScope.runtime : DependencyScope.getScope( scope ),
                                    parseQueryMap( request.query() ) );

            if ( out == null )
            {
                setStatus( ApplicationStatus.NOT_FOUND, request );
            }
            else
            {
                formatOkResponseWithEntity( request, out, text_plain );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }
}
