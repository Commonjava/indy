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
package org.commonjava.aprox.depgraph.jaxrs.filter;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.util.logging.Logger;

@WebFilter( { "/api/1.0/depgraph", "/api/1.0/depgraph/*" } )
public class WorkspaceHandlerFilter
    implements Filter
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private CartoDataManager dataManager;

    @Override
    public void destroy()
    {
    }

    @Override
    public void doFilter( final ServletRequest request, final ServletResponse response, final FilterChain chain )
        throws IOException, ServletException
    {
        GraphWorkspace ws = null;
        try
        {
            final HttpServletRequest req = (HttpServletRequest) request;
            final String wsid = req.getParameter( "wsid" );

            logger.info( "wsid parameter: " + wsid );

            if ( wsid != null )
            {
                logger.info( "Attempting to load workspace: %s into threadlocal...", wsid );

                try
                {
                    ws = dataManager.setCurrentWorkspace( wsid );
                    logger.info( "Got workspace: " + ws );
                }
                catch ( final CartoDataException e )
                {
                    // TODO: This probably shouldn't propagate to the UI...
                    throw new ServletException( "Failed to load workspace: " + wsid, e );
                }
            }

            chain.doFilter( request, response );
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

    @Override
    public void init( final FilterConfig config )
        throws ServletException
    {
    }

}
