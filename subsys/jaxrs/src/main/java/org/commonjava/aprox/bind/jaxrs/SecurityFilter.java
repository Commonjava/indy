package org.commonjava.aprox.bind.jaxrs;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.commonjava.aprox.bind.jaxrs.util.SecurityParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityFilter
    implements Filter
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    //    @Route( "/whoami" )
    //    public void whoami( final HttpServerRequest request )
    //    {
    //        Respond.to( request )
    //               .ok()
    //               .entity( request.params()
    //                               .get( SecurityParam.user.key() ) )
    //               .send();
    //    }

    @Override
    public void destroy()
    {
    }

    @Override
    public void doFilter( final ServletRequest req, final ServletResponse resp, final FilterChain chain )
        throws IOException, ServletException
    {
        logger.info( "Initializing security credentials if necessary..." );

        if ( !( req instanceof HttpServletRequest ) )
        {
            // TODO: How to secure these??
            logger.info( "Not a HTTP servlet request. Skipping." );
            chain.doFilter( req, resp );
        }

        final HttpServletRequest hsr = (HttpServletRequest) req;

        // FIXME: Need a proper login!
        String user = hsr.getRemoteUser();
        if ( user == null )
        {
            user = hsr.getRemoteHost();
        }

        hsr.getSession( true )
           .setAttribute( SecurityParam.user.key(), user );

        chain.doFilter( req, resp );
    }

    @Override
    public void init( final FilterConfig fconfig )
        throws ServletException
    {
    }

}
