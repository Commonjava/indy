package org.commonjava.aprox.depgraph.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.commonjava.util.logging.Logger;

//@WebFilter( "/api/1.0/rel/*" )
public class DebugFilter
    implements Filter
{

    private final Logger logger = new Logger( getClass() );

    @Override
    public void destroy()
    {
        logger.info( "Filter destroy()" );
    }

    @Override
    public void doFilter( final ServletRequest req, final ServletResponse resp, final FilterChain chain )
        throws IOException, ServletException
    {
        final HttpServletRequest request = (HttpServletRequest) req;
        logger.info( "\n\n\n\n\nDEBUG\n\nPath Translated: %s\nPath Info: %s\nContext Path: %s\n\n\n\n",
                     request.getPathTranslated(), request.getPathInfo(), request.getContextPath() );

        chain.doFilter( request, resp );
    }

    @Override
    public void init( final FilterConfig config )
        throws ServletException
    {
        logger.info( "Filter init(..)" );
        //        ctx = config.getServletContext();
    }

}
