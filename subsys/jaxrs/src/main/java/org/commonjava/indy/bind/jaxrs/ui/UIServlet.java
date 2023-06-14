/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.bind.jaxrs.ui;

import org.commonjava.indy.conf.UIConfiguration;
import org.commonjava.indy.util.ApplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.servlet.ServletException;
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
public class UIServlet
    extends AbstractUIServlet
{

    private static final long serialVersionUID = 1L;

    public static final Collection<String> PATHS = Collections.unmodifiableCollection( Arrays.asList( "/*.html", "/",
                                                                                                      "/js/*",
                                                                                                      "/css/*",
                                                                                                      "/partials/*",
                                                                                                      "/ui-addons/*",
                                                                                                      "/swagger.json",
                                                                                                      "/swagger.yaml" ) );

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private UIConfiguration config;

    @Override
    protected void service( final HttpServletRequest request, final HttpServletResponse response )
        throws ServletException, IOException
    {
        if ( config == null )
        {
            config = CDI.current()
                        .select( UIConfiguration.class )
                        .get();
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

        final String method = request.getMethod()
                                     .toUpperCase();

        logger.info( "{} {}", method, path );

        switch ( method )
        {
            case "GET":
            case "HEAD":
            {
                if ( path == null )
                {
                    logger.debug( "null path. Using /index.html" );
                    path = "index.html";
                }
                else if ( path.endsWith( "/" ) )
                {
                    path += "index.html";
                    logger.debug( "directory path. Using {}", path );
                }

                if ( path.startsWith( "/" ) )
                {
                    logger.debug( "Trimming leading '/' from path" );
                    path = path.substring( 1 );
                }

                if ( path.startsWith( "cp/" ) )
                {
                    logger.debug( "Handling request for classpath resource." );
                    path = path.substring( 3 );
                    final URL resource = Thread.currentThread()
                                               .getContextClassLoader()
                                               .getResource( path );

                    sendURL( response, resource, method );
                    return;
                }

                final File uiDir = config.getUIDir();
                logger.debug( "UI basedir: '{}'", uiDir );

                final File resource = new File( uiDir, path );
                if ( !isValidUIResource(uiDir, resource) )
                {
                    logger.debug( "Invalid resource: {}", resource );
                    response.setStatus( ApplicationStatus.BAD_REQUEST.code() );
                    return;
                }

                logger.debug( "Send file: " + resource );
                sendFile( response, resource, method );
                return;
            }
            default:
            {
                logger.error( "Cannot handle request for method: {}", method );
                response.setStatus( ApplicationStatus.BAD_REQUEST.code() );
            }
        }
    }

    /**
     * Check if the resource file is under uiDir in order to prevent path traversal attack.
     * @param uiDir
     * @param resource
     * @return true if requested resource file is under UI dir.
     */
    public static boolean isValidUIResource(File uiDir, File resource)
    {
        try
        {
            return resource.getCanonicalPath().contains(uiDir.getCanonicalPath());
        }
        catch (IOException e)
        {
            final Logger logger = LoggerFactory.getLogger( UIServlet.class );
            logger.warn("Failed to validate UI resource", e);
            return false;
        }
    }

}
