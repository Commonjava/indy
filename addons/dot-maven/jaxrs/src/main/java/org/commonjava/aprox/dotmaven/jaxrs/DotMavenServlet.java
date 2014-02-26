package org.commonjava.aprox.dotmaven.jaxrs;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.exceptions.WebdavException;

import org.commonjava.aprox.dotmaven.webctl.DotMavenService;
import org.commonjava.web.dav.servlet.impl.ServletWebdavRequest;
import org.commonjava.web.dav.servlet.impl.ServletWebdavResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet( { "/mavdav", "/mavdav/", "/mavdav/*", "/mavdav/**" } )
public class DotMavenServlet
    extends HttpServlet
{

    private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private DotMavenService service;

    @Override
    public void service( final HttpServletRequest req, final HttpServletResponse resp )
        throws ServletException, IOException
    {
        try
        {
            service.service( new ServletWebdavRequest( req ), new ServletWebdavResponse( resp ) );
        }
        catch ( final WebdavException e )
        {
            logger.error( "dot-maven request failed: {}", e, e.getMessage() );

            // TODO WebdavException should include a response status code/message.
            resp.sendError( 500, e.getMessage() );
        }
    }

}
