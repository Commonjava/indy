/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.dotmaven.webctl;

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
            logger.error( "ERROR: request has NOT been set in WebDAV RequestInfo: {}", this );
            return null;
        }

        String baseUrl = request.getRequestURI();

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
        return "Indy@" + getHost();
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
