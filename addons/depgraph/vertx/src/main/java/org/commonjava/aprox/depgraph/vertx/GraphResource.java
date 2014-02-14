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
package org.commonjava.aprox.depgraph.vertx;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.setStatus;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_artifactId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_gav;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_groupId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_version;
import static org.commonjava.aprox.util.RequestUtils.parseQueryMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.rest.GraphController;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/depgraph/rel" )
@ApplicationScoped
public class GraphResource
    implements RequestHandler
{
    private final Logger logger = new Logger( getClass() );

    @Inject
    private GraphController controller;

    @Route( "/reindex:?gav=([^/]+/[^/]+/[^/]+)" )
    public void reindex( final HttpServerRequest request )
    {
        final String coord = request.params()
                                    .get( p_gav.key() );
        try
        {
            controller.reindex( coord );
            setStatus( ApplicationStatus.OK, request );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( "/errors:?gav=([^/]+/[^/]+/[^/]+)" )
    public void errors( final HttpServerRequest request )
    {
        final String coord = request.params()
                                    .get( p_gav.key() );
        try
        {
            final String json = controller.errors( coord );
            if ( json != null )
            {
                formatOkResponseWithJsonEntity( request, json );
            }
            else
            {
                setStatus( ApplicationStatus.OK, request );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( "/incomplete:?gav=([^/]+/[^/]+/[^/]+)" )
    public void incomplete( final HttpServerRequest request )
    {
        final String coord = request.params()
                                    .get( p_gav.key() );
        try
        {
            final String json = controller.incomplete( coord, parseQueryMap( request.query() ) );

            if ( json != null )
            {
                formatOkResponseWithJsonEntity( request, json );
            }
            else
            {
                setStatus( ApplicationStatus.OK, request );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( "/variable:?gav=([^/]+/[^/]+/[^/]+)" )
    public void variable( final HttpServerRequest request )
    {
        final String coord = request.params()
                                    .get( p_gav.key() );
        try
        {
            final String json = controller.variable( coord, parseQueryMap( request.query() ) );

            if ( json != null )
            {
                formatOkResponseWithJsonEntity( request, json );
            }
            else
            {
                setStatus( ApplicationStatus.OK, request );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( "/ancestry/:groupId/:artifactId/:version" )
    public void ancestryOf( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );

        try
        {
            final String json = controller.ancestryOf( gid, aid, ver );

            if ( json != null )
            {
                formatOkResponseWithJsonEntity( request, json );
            }
            else
            {
                setStatus( ApplicationStatus.OK, request );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( "/build-order/:groupId/:artifactId/:version" )
    public void buildOrder( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );

        try
        {
            final String json = controller.buildOrder( gid, aid, ver, parseQueryMap( request.query() ) );

            if ( json != null )
            {
                formatOkResponseWithJsonEntity( request, json );
            }
            else
            {
                setStatus( ApplicationStatus.OK, request );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( "/project/:groupId/:artifactId/:version" )
    public void projectGraph( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );

        try
        {
            final String json = controller.projectGraph( gid, aid, ver, parseQueryMap( request.query() ) );

            if ( json != null )
            {
                formatOkResponseWithJsonEntity( request, json );
            }
            else
            {
                setStatus( ApplicationStatus.OK, request );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

}
