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
package org.commonjava.aprox.bind.jaxrs.webctl;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@WebFilter( "/*" )
public class DebugFilter
    implements Filter
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void destroy()
    {
        //        logger.info( "Filter destroy()" );
    }

    @Override
    public void doFilter( final ServletRequest req, final ServletResponse resp, final FilterChain chain )
        throws IOException, ServletException
    {
        final HttpServletRequest request = (HttpServletRequest) req;
        logger.info( "REQUEST:\n  URI: {}\n  Path Translated: {}\n  Path Info: {}\n  Context Path: {}\n\n", request.getRequestURI(),
                     request.getPathTranslated(), request.getPathInfo(), request.getContextPath() );

        chain.doFilter( request, resp );
    }

    @Override
    public void init( final FilterConfig config )
        throws ServletException
    {
        //        logger.info( "Filter init(..)" );
        //        ctx = config.getServletContext();
    }

}
