package org.commonjava.aprox.core.rest;

import static org.apache.commons.lang.StringUtils.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.commonjava.util.logging.Logger;

public abstract class AbstractURLAliasingResource
{

    private final Logger logger = new Logger( getClass() );

    @Context
    private HttpServletRequest request;

    @Context
    private HttpServletResponse response;

    @Context
    private UriInfo uriInfo;

    private String baseUri;

    protected synchronized void forward( final String... paths )
        throws ServletException, IOException
    {
        if ( baseUri == null )
        {
            baseUri = request.getServletContext()
                             .getContextPath();
            if ( !baseUri.endsWith( "/" ) )
            {
                baseUri += "/";
            }
        }

        logger.info( "\n\nBase URL for deprecated path forward:\n    %s\n\n", baseUri );

        final List<String> allPathList = new ArrayList<String>();
        for ( final String path : paths )
        {
            final String[] parts = path.split( "/" );
            for ( final String part : parts )
            {
                if ( part.trim()
                         .length() > 0 )
                {
                    allPathList.add( part );
                }
            }
        }

        final String[] allPaths = allPathList.toArray( new String[] {} );

        String url = uriInfo.getRequestUriBuilder()
                            .segment( allPaths )
                            .build()
                            .getPath();

        logger.info( "\n\nFull forward URL:\n    %s\n\n", url );

        url = url.substring( baseUri.length() );

        final LinkedList<String> parts = new LinkedList<String>();
        for ( final String part : url.split( "/" ) )
        {
            if ( part.trim()
                     .length() < 1 )
            {
                continue;
            }
            else if ( !parts.isEmpty() && "..".equals( part ) )
            {
                parts.removeLast();
            }
            else
            {
                parts.add( part );
            }
        }

        url = "/" + join( parts, "/" );
        logger.info( "\n\nRelative forward URL:\n    %s\n\n", url );

        request.getRequestDispatcher( url )
               .forward( request, response );
    }

}
