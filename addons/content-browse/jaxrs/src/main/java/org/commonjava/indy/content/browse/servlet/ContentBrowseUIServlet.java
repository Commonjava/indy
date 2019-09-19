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
package org.commonjava.indy.content.browse.servlet;

import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.bind.jaxrs.ui.AbstractUIServlet;
import org.commonjava.indy.conf.UIConfiguration;
import org.commonjava.indy.content.browse.conf.ContentBrowseConfig;
import org.commonjava.indy.util.ApplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@ApplicationScoped
@WebServlet
public class ContentBrowseUIServlet
        extends AbstractUIServlet
{
    private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final Collection<String> PATHS = Collections.unmodifiableCollection( Arrays.asList( "/browse/*" ) );

    @Inject
    private ContentBrowseConfig config;

    @Override
    protected void service( final HttpServletRequest request, final HttpServletResponse response )
            throws ServletException, IOException
    {
        if ( config == null )
        {
            config = CDI.current().select( ContentBrowseConfig.class ).get();
        }

        String path;
        try
        {
            path = new URI( request.getRequestURI() ).getPath();
        }
        catch ( final URISyntaxException e )
        {
            logger.error( "Cannot parse request URI", e );
            response.setStatus( 400 );
            return;
        }

        final String method = request.getMethod().toUpperCase();

        logger.info( "{} {}", method, path );

        path = path.replace( "browse/", "" );

        switch ( method )
        {
            case "GET":
            case "HEAD":
            {
                if ( path.startsWith( "/" ) )
                {
                    logger.debug( "Trimming leading '/' from path" );
                    path = path.substring( 1 );
                }

                if ( !path.equals( "app_bundle.js" ) )
                {
                    logger.debug( "All path which is not requesting .js will rewrite to index.html" );
                    path = "index.html";
                }

                final File uiDir = config.getContentBrowseUIDir();
                logger.debug( "Content Browse UI basedir: '{}'", uiDir );

                final File resource = new File( uiDir, path );
                logger.debug( "Trying to send file: " + resource );
                sendFile( response, resource, method );
                return;
            }
            default:
            {
                logger.error( "cannot handle request for method: {}", method );
                response.setStatus( ApplicationStatus.BAD_REQUEST.code() );
            }
        }
    }
}
