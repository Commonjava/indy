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
package org.commonjava.aprox.util;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public final class UrlUtils
{

    private UrlUtils()
    {
    }

    public static String stringQueryParameter( final Object value )
    {
        final String base = String.valueOf( value );
        return "%22" + base + "%22";
    }

    public static String siblingDatabaseUrl( final String dbUrl, final String siblingName )
    {
        if ( isEmpty( dbUrl ) )
        {
            throw new IllegalArgumentException(
                                                "Cannot calculate sibling database URL based on empty or null database URL." );
        }

        final StringBuilder sb = new StringBuilder();
        final int protoIdx = dbUrl.indexOf( "://" ) + 3;

        final int lastIdx;
        if ( dbUrl.charAt( dbUrl.length() - 1 ) == '/' )
        {
            lastIdx = dbUrl.lastIndexOf( '/', dbUrl.length() - 2 );
        }
        else
        {
            lastIdx = dbUrl.lastIndexOf( '/' );
        }

        if ( lastIdx > protoIdx )
        {
            sb.append( dbUrl.substring( 0, lastIdx + 1 ) )
              .append( siblingName );

            return sb.toString();
        }

        throw new IllegalArgumentException( "Cannot calculate sibling database URL for: '" + dbUrl
            + "' (cannot find last path separator '/')" );
    }

    public static String buildUrl( final String baseUrl, final String... parts )
        throws MalformedURLException
    {
        return buildUrl( baseUrl, null, parts );
    }

    public static String buildUrl( final String baseUrl, final Map<String, String> params, final String... parts )
        throws MalformedURLException
    {
        if ( parts == null || parts.length < 1 )
        {
            return baseUrl;
        }

        final StringBuilder urlBuilder = new StringBuilder();

        if ( parts[0] == null || !parts[0].startsWith( baseUrl ) )
        {
            urlBuilder.append( baseUrl );
        }

        for ( String part : parts )
        {
            if ( part == null || part.trim()
                                     .length() < 1 )
            {
                continue;
            }

            if ( part.startsWith( "/" ) )
            {
                part = part.substring( 1 );
            }

            if ( urlBuilder.length() > 0 && urlBuilder.charAt( urlBuilder.length() - 1 ) != '/' )
            {
                urlBuilder.append( "/" );
            }

            urlBuilder.append( part );
        }

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

        return new URL( urlBuilder.toString() ).toExternalForm();
    }

    public static UrlInfo parseUrlInfo( final String url )
    {
        return new UrlInfo( url );
    }
}
