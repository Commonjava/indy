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
package org.commonjava.aprox.depgraph.vertx.resolve;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_artifactId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_from;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_groupId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_version;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.q_recurse;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParamUtils.getWorkspaceId;
import static org.commonjava.aprox.model.util.HttpUtils.parseQueryMap;
import static org.commonjava.aprox.model.util.HttpUtils.toBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.rest.ResolverController;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.util.Respond;
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
            final String json =
                controller.resolveGraph( f, gid, aid, ver, recurse, getWorkspaceId( request ),
                                         parseQueryMap( request.query() ) );
            if ( json == null )
            {
                Respond.to( request )
                       .ok()
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
            controller.resolveIncomplete( f, gid, aid, ver, recurse, getWorkspaceId( request ),
                                          parseQueryMap( request.query() ) );
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

}
