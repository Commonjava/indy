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
package org.commonjava.indy.client.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.apache.commons.lang.StringUtils.join;

public final class UrlUtils
{

    private UrlUtils()
    {
    }

    public static String buildUrl( final String baseUrl, final String... parts )
    {
        return buildUrl( baseUrl, null, parts );
    }

    public static String buildUrl( final String baseUrl, final Supplier<Map<String, String>> paramSupplier, final String... parts )
    {
        Logger logger = LoggerFactory.getLogger( UrlUtils.class );
        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Creating url from base: '{}' and parts: {}", baseUrl, join( parts, ", " ) );
        }

        if ( parts == null || parts.length < 1 )
        {
            return baseUrl;
        }

        final StringBuilder urlBuilder = new StringBuilder();

        final List<String> list = new ArrayList<>();

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

        if ( paramSupplier != null )
        {
            Map<String, String> params = paramSupplier.get();

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
                continue;
            }

            if ( idx == 0 && part.startsWith( "file:" ) )
            {
                if ( part.length() > 5 )
                {
                    sb.append( part.substring( 5 ) );
                }

                continue;
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
