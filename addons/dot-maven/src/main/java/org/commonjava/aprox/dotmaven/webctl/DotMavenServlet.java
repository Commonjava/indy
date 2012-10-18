package org.commonjava.aprox.dotmaven.webctl;

import io.milton.http.HttpManager;
import io.milton.servlet.MiltonServlet;

import java.io.IOException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

@WebServlet( displayName = "dotMaven-Servlet", name = "dot-maven", urlPatterns = { "/mavdav", "/mavdav/*" } )
@RequestScoped
public class DotMavenServlet
    extends MiltonServlet
{
    public static final String NAME = "mavdav";

    //    private final Logger logger = new Logger( getClass() );

    @Inject
    private HttpManager injectedManager;

    @Inject
    private RequestInfo requestInfo;

    @Override
    public void init( final ServletConfig config )
        throws ServletException
    {
        super.init( config );

        httpManager = injectedManager;
    }

    @Override
    public void service( final ServletRequest servletRequest, final ServletResponse servletResponse )
        throws ServletException, IOException
    {
        final HttpServletRequest hsr = (HttpServletRequest) servletRequest;
        requestInfo.setRequest( hsr );

        super.service( servletRequest, servletResponse );
    }

}
