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

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.setStatus;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_artifactId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_gav;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_groupId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_version;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParamUtils.getWorkspaceId;
import static org.commonjava.aprox.util.RequestUtils.parseQueryMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.rest.GraphController;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/depgraph/rel" )
@ApplicationScoped
public class GraphResource
    implements RequestHandler
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private GraphController controller;

    @Route( "/reindex:?gav=([^/]+/[^/]+/[^/]+)" )
    public void reindex( final HttpServerRequest request )
    {
        final String coord = request.params()
                                    .get( p_gav.key() );
        try
        {
            controller.reindex( coord, getWorkspaceId( request ) );
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
            final String json = controller.errors( coord, getWorkspaceId( request ) );
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
            final String json =
                controller.incomplete( coord, getWorkspaceId( request ), parseQueryMap( request.query() ) );

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
            final String json =
                controller.variable( coord, getWorkspaceId( request ), parseQueryMap( request.query() ) );

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
            final String json = controller.ancestryOf( gid, aid, ver, getWorkspaceId( request ) );

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
            final String json =
                controller.buildOrder( gid, aid, ver, getWorkspaceId( request ), parseQueryMap( request.query() ) );

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
            final String json =
                controller.projectGraph( gid, aid, ver, getWorkspaceId( request ), parseQueryMap( request.query() ) );

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
