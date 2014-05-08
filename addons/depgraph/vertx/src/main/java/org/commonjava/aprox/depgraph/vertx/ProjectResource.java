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
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_groupId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_version;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.q_artifactId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.q_groupId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.q_scopes;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParamUtils.getWorkspaceId;
import static org.commonjava.aprox.util.RequestUtils.parseQueryMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.rest.ProjectController;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/depgraph/project" )
@ApplicationScoped
public class ProjectResource
    implements RequestHandler
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ProjectController controller;

    @Route( "/:groupId/:artifactId/:version/errors" )
    public void errors( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );

        String json = null;
        try
        {
            json = controller.errors( gid, aid, ver, getWorkspaceId( request ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }

        if ( json != null )
        {
            formatOkResponseWithJsonEntity( request, json );
        }
        else
        {
            setStatus( ApplicationStatus.NO_CONTENT, request );
        }
    }

    @Route( "/list" )
    public void list( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String groupIdPattern = params.get( q_groupId.key() );
        final String artifactIdPattern = params.get( q_artifactId.key() );

        String json = null;
        try
        {
            json = controller.list( groupIdPattern, artifactIdPattern, getWorkspaceId( request ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }

        if ( json != null )
        {
            formatOkResponseWithJsonEntity( request, json );
        }
        else
        {
            setStatus( ApplicationStatus.NO_CONTENT, request );
        }
    }

    @Route( "/:groupId/:artifactId/:version/parent" )
    public void parentOf( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );

        String json = null;
        try
        {
            json = controller.parentOf( gid, aid, ver, getWorkspaceId( request ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }

        if ( json != null )
        {
            formatOkResponseWithJsonEntity( request, json );
        }
        else
        {
            setStatus( ApplicationStatus.NO_CONTENT, request );
        }
    }

    @Route( "/:groupId/:artifactId/:version/dependencies" )
    public void dependenciesOf( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );
        final String scopesStr = params.get( q_scopes.key() );

        String json = null;
        try
        {
            json =
                controller.dependenciesOf( gid, aid, ver, getWorkspaceId( request ),
                                           DependencyScope.parseScopes( scopesStr ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }

        if ( json != null )
        {
            formatOkResponseWithJsonEntity( request, json );
        }
        else
        {
            setStatus( ApplicationStatus.NO_CONTENT, request );
        }
    }

    @Route( "/:groupId/:artifactId/:version/plugins" )
    public void pluginsOf( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );

        String json = null;
        try
        {
            json =
                controller.relationshipsDeclaredBy( gid, aid, ver, getWorkspaceId( request ), RelationshipType.PLUGIN );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }

        if ( json != null )
        {
            formatOkResponseWithJsonEntity( request, json );
        }
        else
        {
            setStatus( ApplicationStatus.NO_CONTENT, request );
        }
    }

    @Route( "/:groupId/:artifactId/:version/extensions" )
    public void extensionsOf( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );

        String json = null;
        try
        {
            json =
                controller.relationshipsDeclaredBy( gid, aid, ver, getWorkspaceId( request ),
                                                    RelationshipType.EXTENSION );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }

        if ( json != null )
        {
            formatOkResponseWithJsonEntity( request, json );
        }
        else
        {
            setStatus( ApplicationStatus.NO_CONTENT, request );
        }
    }

    @Route( "/:groupId/:artifactId/:version/relationships" )
    public void relationshipsSpecifiedBy( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );

        String json = null;
        try
        {
            json =
                controller.relationshipsTargeting( gid, aid, ver, getWorkspaceId( request ),
                                                   parseQueryMap( request.query() ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }

        if ( json != null )
        {
            formatOkResponseWithJsonEntity( request, json );
        }
        else
        {
            setStatus( ApplicationStatus.NO_CONTENT, request );
        }
    }

    @Route( "/:groupId/:artifactId/:version/users" )
    public void relationshipsTargeting( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );

        String json = null;
        try
        {
            json =
                controller.relationshipsTargeting( gid, aid, ver, getWorkspaceId( request ),
                                                   parseQueryMap( request.query() ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }

        if ( json != null )
        {
            formatOkResponseWithJsonEntity( request, json );
        }
        else
        {
            setStatus( ApplicationStatus.NO_CONTENT, request );
        }
    }

}
