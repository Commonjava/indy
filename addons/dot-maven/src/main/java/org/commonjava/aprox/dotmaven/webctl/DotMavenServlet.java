package org.commonjava.aprox.dotmaven.webctl;

import java.io.IOException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.sf.webdav.IWebdavStore;
import net.sf.webdav.WebdavServlet;

@WebServlet( displayName = "dotMaven-Servlet", name = "dot-maven", urlPatterns = { "/mavdav", "/mavdav/*" } )
@RequestScoped
public class DotMavenServlet
    extends WebdavServlet
{
    public static final String NAME = "mavdav";

    //    private final Logger logger = new Logger( getClass() );

    private static final long serialVersionUID = 1L;

    private static final String EMPTY_STRING = "";

    @Inject
    private RequestInfo requestInfo;

    @Inject
    private IWebdavStore webdavStore;

    @Override
    public void init()
        throws ServletException
    {
        final boolean lazyFolderCreationOnPut = true; // create intermediary folders, I believe...

        final String dftIndexFile = EMPTY_STRING; // Can I improve on this??
        final String insteadOf404 = EMPTY_STRING; // How is this used in practice?

        final int noContentLengthHeader = 0; // TODO: What are these for??

        super.init( webdavStore, dftIndexFile, insteadOf404, noContentLengthHeader, lazyFolderCreationOnPut );
    }

    @Override
    public void service( final ServletRequest servletRequest, final ServletResponse servletResponse )
        throws ServletException, IOException
    {
        final HttpServletRequest hsr = (HttpServletRequest) servletRequest;
        final String mount = hsr.getParameter( RequestInfo.MOUNT_POINT );
        final HttpSession session = hsr.getSession( true );
        session.setAttribute( RequestInfo.MOUNT_POINT, mount == null ? "/tmp/mavdav" : mount );

        requestInfo.setRequest( hsr );

        super.service( servletRequest, servletResponse );
    }

}
