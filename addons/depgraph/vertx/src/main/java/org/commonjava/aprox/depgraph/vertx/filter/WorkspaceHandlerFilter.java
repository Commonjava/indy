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
package org.commonjava.aprox.depgraph.vertx.filter;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_wsid;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.q_wsid;

import javax.inject.Inject;

import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.vertx.vabr.anno.FilterRoute;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.filter.ExecutionChain;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( "workspaceFilter" )
public class WorkspaceHandlerFilter
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CartoDataManager dataManager;

    @FilterRoute( path = "/depgraph.*", method = Method.ANY )
    public void filter( final HttpServerRequest request, final ExecutionChain chain )
        throws Exception
    {
        GraphWorkspace ws = null;
        try
        {
            String wsid = request.params()
                                 .get( p_wsid.key() );
            if ( wsid == null )
            {
                wsid = request.params()
                              .get( q_wsid.key() );
            }

            logger.info( "wsid parameter: " + wsid );

            if ( wsid != null )
            {
                logger.info( "Attempting to load workspace: {} into threadlocal...", wsid );

                try
                {
                    ws = dataManager.setCurrentWorkspace( wsid );
                    logger.info( "Got workspace: " + ws );
                }
                catch ( final CartoDataException e )
                {
                    formatResponse( e, request );

                    //prevent further work.
                    return;
                }
            }

            chain.handle();
        }
        finally
        {
            // detach from the threadlocal...
            logger.info( "Detaching workspace: " + ws );
            try
            {
                dataManager.clearCurrentWorkspace();
            }
            catch ( final CartoDataException e )
            {
                logger.error( e.getMessage(), e );
            }
        }
    }

}
