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
package org.commonjava.aprox.dotmaven.webctl;

import java.net.MalformedURLException;
import java.net.URL;

import javax.enterprise.context.ApplicationScoped;

import net.sf.webdav.spi.WebdavRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RequestInfo
{

    public static final String MOUNT_POINT = "mount";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

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

        logger.debug( "DAV RAW Base-URL: {}", baseUrl );

        final int idx = baseUrl.indexOf( DotMavenService.NAME );
        if ( idx > 0 )
        {
            baseUrl = baseUrl.substring( 0, idx );
        }
        else
        {
            baseUrl = null;
        }

        logger.debug( "DAV Processed Base-URL: {}", baseUrl );

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
