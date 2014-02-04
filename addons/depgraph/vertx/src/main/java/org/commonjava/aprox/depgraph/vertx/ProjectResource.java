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
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_groupId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_version;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.q_artifactId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.q_groupId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.q_scopes;
import static org.commonjava.aprox.rest.util.RequestUtils.parseQueryMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.depgraph.rest.ProjectController;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/depgraph/project" )
@ApplicationScoped
public class ProjectResource
    implements RequestHandler
{
    private final Logger logger = new Logger( getClass() );

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
            json = controller.errors( gid, aid, ver );
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
            json = controller.list( groupIdPattern, artifactIdPattern );
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
            json = controller.parentOf( gid, aid, ver );
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
            json = controller.dependenciesOf( gid, aid, ver, DependencyScope.parseScopes( scopesStr ) );
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
            json = controller.pluginsOf( gid, aid, ver );
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
            json = controller.extensionsOf( gid, aid, ver );
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
            json = controller.relationshipsSpecifiedBy( gid, aid, ver, parseQueryMap( request.query() ) );
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
            json = controller.relationshipsTargeting( gid, aid, ver, parseQueryMap( request.query() ) );
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
