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
package org.commonjava.aprox.filer;

import java.io.File;

public final class PathUtils
{
    private PathUtils()
    {
    }

    public static String join( final String base, final String... parts )
    {
        if ( parts.length < 1 )
        {
            return base;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append( base );

        for ( final String part : parts )
        {
            final String[] subParts = part.split( "/" );
            for ( final String subPart : subParts )
            {
                final String normal = normalizePathPart( subPart );
                if ( normal.length() < 1 )
                {
                    continue;
                }

                sb.append( "/" )
                  .append( normal );
            }
        }

        return sb.toString();
    }

    public static String normalizePathPart( final String path )
    {
        String result = path.trim();
        while ( result.startsWith( "/" ) && result.length() > 1 )
        {
            result = result.substring( 1 );
        }

        return result.replace( '\\', '/' );
    }

    public static String dirname( final String path )
    {
        if ( path == null )
        {
            return null;
        }

        if ( path.endsWith( "/" ) )
        {
            return path;
        }

        return new File( path ).getParent();
    }
}
