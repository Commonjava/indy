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
package org.commonjava.aprox.depgraph.vertx.resolve;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.setStatus;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_artifactId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_from;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_groupId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_version;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.q_recurse;
import static org.commonjava.aprox.rest.util.RequestUtils.parseQueryMap;
import static org.commonjava.aprox.rest.util.RequestUtils.toBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.depgraph.rest.ResolverController;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/depgraph/resolve/:from=(.+)" )
@ApplicationScoped
public class ResolverResource
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ResolverController controller;

    @Route( "/:groupId/:artifactId/:version" )
    public void resolveGraph( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String f = params.get( p_from.key() );
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );
        final boolean recurse = toBoolean( params.get( q_recurse.key() ), false );

        try
        {
            final String json = controller.resolveGraph( f, gid, aid, ver, recurse, parseQueryMap( request.query() ) );
            if ( json == null )
            {
                setStatus( ApplicationStatus.OK, request );
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

    @Route( "/:groupId/:artifactId/:version/incomplete" )
    public void resolveIncomplete( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String f = params.get( p_from.key() );
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );
        final boolean recurse = toBoolean( params.get( q_recurse.key() ), false );

        try
        {
            final String json = controller.resolveIncomplete( f, gid, aid, ver, recurse, parseQueryMap( request.query() ) );
            if ( json == null )
            {
                setStatus( ApplicationStatus.OK, request );
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

}
