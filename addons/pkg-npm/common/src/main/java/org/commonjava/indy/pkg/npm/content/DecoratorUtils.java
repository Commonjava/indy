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
package org.commonjava.indy.pkg.npm.content;

import org.commonjava.maven.galley.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class DecoratorUtils
{
    private static final Logger logger = LoggerFactory.getLogger( DecoratorUtils.class );

    /**
     * Replace tarball urls with context urls, e.g., "https://registry.npmjs.org/jquery/-/jquery-1.5.1.tgz" to
     * "http://${indy}/api/content/npm/remote/test/jquery/-/jquery-1.5.1.tgz".
     */
    public static String updatePackageJson( String raw, String contextURL )
            throws IOException
    {
        final String TARBALL = "\"tarball\"";
        StringBuilder sb = new StringBuilder();
        int index;
        while ( ( index = raw.indexOf( TARBALL ) ) >= 0 )
        {
            int colon = raw.indexOf( ":", index );
            boolean found;
            int indexWithTarball = index + TARBALL.length();
            String lag;
            if ( indexWithTarball == colon )
            {
                found = true; // there is no extra characters between "tarball" and :
            }
            else
            {
                lag = raw.substring( indexWithTarball, colon );
                found = "".equals( lag.trim() ); // only whitespace between "tarball" and :
            }
            if ( !found )
            {   // This means found "tarball" with no trailing colon, not the real case to replace
                sb.append( raw, 0, indexWithTarball );
                raw = raw.substring( indexWithTarball );
                continue;
            }

            final String before = raw.substring( 0, colon + 1 );
            sb.append( before );
            raw = raw.substring( colon + 1 );

            int quote = raw.indexOf( "\"" );
            final String extra = raw.substring( 0, quote ); // blanks between : and "
            sb.append( extra );

            int nextQuote = raw.indexOf( "\"", quote + 1 );

            String url = raw.substring( quote + 1, nextQuote );
            String path = getPath( url );
            if ( path != null )
            {
                url = UrlUtils.buildUrl( contextURL, path );
            }
            final String value = "\"" + url + "\"";
            sb.append( value );
            raw = raw.substring( nextQuote + 1 );
        }
        sb.append( raw );
        return sb.toString();
    }

    private static String getPath( String url )
    {
        URL url1;
        try
        {
            url1 = new URL( url );
        }
        catch ( MalformedURLException e )
        {
            logger.warn( "Failed to parse URL {}" + url );
            return null;
        }

        String[] pathParts = url1.getPath().split( "/" );
        if ( pathParts.length < 1 )
        {
            return "";
        }
        else if ( pathParts.length == 1 )
        {
            return pathParts[0];
        }

        String lastPart = pathParts[pathParts.length - 1];
        if ( ( "package.json".equals( lastPart ) || lastPart.endsWith( "tgz" ) ) && pathParts.length > 2 )
        {
            final String firstPath;
            //Handle if scopedPath like "@types/jquery/***" or singlePath like "jquery/***"
            if ( pathParts.length > 3 && pathParts[pathParts.length - 4].startsWith( "@" ) )
            {
                // scoped path
                firstPath = String.format( "%s/%s", pathParts[pathParts.length - 4], pathParts[pathParts.length - 3] );
            }
            else
            {
                // single path
                firstPath = pathParts[pathParts.length - 3];
            }
            return String.format( "%s/%s/%s", firstPath, pathParts[pathParts.length - 2],
                                  pathParts[pathParts.length - 1] );
        }
        else if ( "-".equals( lastPart ) )
        {
            final String firstPath;
            //Handle if scopedPath like "@types/jquery/***" or singlePath like "jquery/***"
            if ( pathParts.length > 2 && pathParts[pathParts.length - 3].startsWith( "@" ) )
            {
                // scoped path
                firstPath = String.format( "%s/%s", pathParts[pathParts.length - 3], pathParts[pathParts.length - 2] );
            }
            else
            {
                // single path
                firstPath = pathParts[pathParts.length - 2];
            }
            return String.format( "%s/%s", firstPath, pathParts[pathParts.length - 1] );
        }

        return lastPart;

    }

}
