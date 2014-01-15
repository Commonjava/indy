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
package org.commonjava.aprox.dotmaven.webctl;

import java.net.MalformedURLException;
import java.net.URL;

import javax.enterprise.context.RequestScoped;

import net.sf.webdav.spi.WebdavRequest;

@RequestScoped
public class RequestInfo
{

    public static final String MOUNT_POINT = "mount";

    //    private final Logger logger = new Logger( getClass() );

    private WebdavRequest request;

    public WebdavRequest getRequest()
    {
        return request;
    }

    public void setRequest( final WebdavRequest request )
    {
        this.request = request;
    }

    public String getBaseUrl()
    {
        if ( request == null )
        {
            return null;
        }

        String baseUrl = request.getRequestURI()
                                .toString();
        //        logger.info( "Request: %s", baseUrl );

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
