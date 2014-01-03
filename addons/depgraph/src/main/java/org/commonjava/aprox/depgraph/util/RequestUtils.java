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
package org.commonjava.aprox.depgraph.util;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.util.logging.Logger;

public final class RequestUtils
{

    private static final Logger logger = new Logger( RequestUtils.class );

    private RequestUtils()
    {
    }

    public static ProjectVersionRef parseGAV( final String src )
    {
        String gav = src;
        if ( gav.startsWith( "/" ) )
        {
            if ( gav.length() > 1 )
            {
                gav = gav.substring( 1 );
            }
            else
            {
                return null;
            }
        }

        if ( gav.trim()
                .length() < 1 )
        {
            return null;
        }

        final String[] parts = gav.split( "\\/" );
        if ( parts.length != 3 )
        {
            throw new WebApplicationException(
                                               Response.status( BAD_REQUEST )
                                                       .entity( "GAV sub-path should be of the form 'some.group.id/artifact-id/version'. Instead, I got: '"
                                                                    + src + "'." )
                                                       .build() );
        }

        final ProjectVersionRef ref = new ProjectVersionRef( parts[0], parts[1], parts[2] );
        return ref;
    }

    public static String getStringParamWithDefault( final HttpServletRequest request, final String key, final String def )
    {
        final String value = request.getParameter( key );
        String val = value;
        if ( val == null || val.trim()
                               .length() < 1 )
        {
            val = def;
        }

        logger.info( "Value of key: %s is: %s. Returning string-param-with-default value: %s", key, value, val );
        return val;
    }

    public static long getLongParamWithDefault( final HttpServletRequest request, final String key, final long def )
    {
        final String value = request.getParameter( key );
        long val;
        if ( value == null || value.trim()
                                   .length() < 1 )
        {
            val = def;
        }
        else
        {
            val = Long.parseLong( value );
        }

        logger.info( "Value of key: %s is: %s. Returning long-param-with-default value: %s", key, value, val );
        return val;
    }

    public static boolean getBooleanParamWithDefault( final HttpServletRequest request, final String key,
                                                      final boolean def )
    {
        final String value = request.getParameter( key );
        boolean val;
        if ( value == null || value.trim()
                                   .length() < 1 )
        {
            val = def;
        }
        else
        {
            val = Boolean.parseBoolean( value );
        }

        logger.info( "Value of key: %s is: %s. Returning boolean-param-with-default value: %s", key, value, val );
        return val;
    }

}
