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
package org.commonjava.aprox.bind.jaxrs.util;

import static org.apache.commons.lang.StringUtils.join;

import java.net.MalformedURLException;

import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.galley.util.PathUtils;
import org.commonjava.maven.galley.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaxRsUriFormatter
    implements UriFormatter
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public String formatAbsolutePathTo( final String base, final String... parts )
    {
        logger.debug( "Formatting URL from base: '{}' and parts: {}", base, new JoinString( ", ", parts ) );

        String url = null;
        try
        {
            url = UrlUtils.buildUrl( base, parts );
        }
        catch ( final MalformedURLException e )
        {
            logger.warn( "Failed to use UrlUtils to build URL from base: {} and parts: {}", base, join( parts, ", " ) );
            url = PathUtils.normalize( base, PathUtils.normalize( parts ) );
        }

        if ( url.length() > 0 && !url.matches( "[a-zA-Z0-9]+\\:\\/\\/.+" ) && url.charAt( 0 ) != '/' )
        {
            url = "/" + url;
        }

        logger.debug( "Resulting URL: '{}'", url );

        return url;

        //        URL baseUrl = null;
        //        String path = base;
        //        try
        //        {
        //            baseUrl = new URL( base );
        //            path = baseUrl.getPath();
        //        }
        //        catch ( MalformedURLException e )
        //        {
        //            // not a URL.
        //        }
        //        
        //        path = PathUtils.normalize( base, PathUtils.normalize( parts ) );
        //        if ( !path.startsWith( "/" ) )
        //        {
        //            path = "/" + path;
        //        }
        //        
        //        if ( baseUrl != null )
        //        {
        //            // reconstruct...
        //        }
        //
        //        return path;
    }

}
