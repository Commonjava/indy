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
package org.commonjava.aprox.client.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class UrlUtils
{

    private UrlUtils()
    {
    }

    public static String buildUrl( final String baseUrl, final String... parts )
    {
        return buildUrl( baseUrl, null, parts );
    }

    public static String buildUrl( final String baseUrl, final Map<String, String> params, final String... parts )
    {
        if ( parts == null || parts.length < 1 )
        {
            return baseUrl;
        }

        final StringBuilder urlBuilder = new StringBuilder();

        final List<String> list = new ArrayList<String>();

        if ( baseUrl != null && !"null".equals( baseUrl ) )
        {
            list.add( baseUrl );
        }
        else
        {
            list.add( "/" );
        }

        for ( final String part : parts )
        {
            if ( part == null || "null".equals( part ) )
            {
                continue;
            }

            list.add( part );
        }

        urlBuilder.append( normalizePath( list.toArray( new String[list.size()] ) ) );
        //
        //        if ( parts[0] == null || !parts[0].startsWith( baseUrl ) )
        //        {
        //            urlBuilder.append( baseUrl );
        //        }
        //
        //        for ( String part : parts )
        //        {
        //            if ( part == null || part.trim()
        //                                     .length() < 1 )
        //            {
        //                continue;
        //            }
        //
        //            if ( part.startsWith( "/" ) )
        //            {
        //                part = part.substring( 1 );
        //            }
        //
        //            if ( urlBuilder.length() > 0 && urlBuilder.charAt( urlBuilder.length() - 1 ) != '/' )
        //            {
        //                urlBuilder.append( "/" );
        //            }
        //
        //            urlBuilder.append( part );
        //        }

        if ( params != null && !params.isEmpty() )
        {
            urlBuilder.append( "?" );
            boolean first = true;
            for ( final Map.Entry<String, String> param : params.entrySet() )
            {
                if ( first )
                {
                    first = false;
                }
                else
                {
                    urlBuilder.append( "&" );
                }

                urlBuilder.append( param.getKey() )
                          .append( "=" )
                          .append( param.getValue() );
            }
        }

        return urlBuilder.toString();
    }

    public static String normalizePath( final String... path )
    {
        if ( path == null || path.length < 1 )
        {
            return "/";
        }

        final StringBuilder sb = new StringBuilder();
        int idx = 0;
        parts: for ( String part : path )
        {
            if ( part == null || part.length() < 1 || "/".equals( part ) )
            {
                continue parts;
            }

            if ( idx == 0 && part.startsWith( "file:" ) )
            {
                if ( part.length() > 5 )
                {
                    sb.append( part.substring( 5 ) );
                }

                continue parts;
            }

            if ( idx > 0 )
            {
                while ( part.charAt( 0 ) == '/' )
                {
                    if ( part.length() < 2 )
                    {
                        continue parts;
                    }

                    part = part.substring( 1 );
                }
            }

            while ( part.charAt( part.length() - 1 ) == '/' )
            {
                if ( part.length() < 2 )
                {
                    continue parts;
                }

                part = part.substring( 0, part.length() - 1 );
            }

            if ( sb.length() > 0 )
            {
                sb.append( '/' );
            }

            sb.append( part );
            idx++;
        }

        if ( path[path.length - 1].endsWith( "/" ) )
        {
            sb.append( "/" );
        }

        return sb.toString();
    }

}
