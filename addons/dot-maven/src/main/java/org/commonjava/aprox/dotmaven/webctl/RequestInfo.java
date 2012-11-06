package org.commonjava.aprox.dotmaven.webctl;

import java.net.MalformedURLException;
import java.net.URL;

import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;

import org.commonjava.util.logging.Logger;

@RequestScoped
public class RequestInfo
{

    public static final String MOUNT_POINT = "mount";

    private final Logger logger = new Logger( getClass() );

    private HttpServletRequest request;

    public HttpServletRequest getRequest()
    {
        return request;
    }

    public void setRequest( final HttpServletRequest request )
    {
        this.request = request;
    }

    public String getBaseUrl()
    {
        if ( request == null )
        {
            return null;
        }

        String baseUrl = request.getRequestURL()
                                .toString();
        logger.info( "Request: %s", baseUrl );

        final int idx = baseUrl.indexOf( DotMavenServlet.NAME );
        if ( idx > 0 )
        {
            baseUrl = baseUrl.substring( 0, idx );
        }
        else
        {
            baseUrl = null;
        }

        return baseUrl;
    }

    public String getRealm()
    {
        return "AProx@" + getHost();
    }

    public String getHost()
    {
        String host;
        try
        {
            host = new URL( request.getRequestURI() ).getHost();
        }
        catch ( final MalformedURLException e )
        {
            host = request.getServerName();
        }

        return host;
    }

}
