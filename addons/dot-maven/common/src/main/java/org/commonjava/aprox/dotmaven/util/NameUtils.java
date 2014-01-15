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
package org.commonjava.aprox.dotmaven.util;

import static org.apache.commons.lang.StringUtils.join;

import java.io.File;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;

public final class NameUtils
{

    private static final String[] BANNED_RESOURCE_PATTERNS = { "\\/?\\..+", };

    private NameUtils()
    {
    }

    public static boolean isValidResource( final String name )
    {
        final File f = new File( name );
        final String fname = f.getName();
        for ( final String bannedPattern : BANNED_RESOURCE_PATTERNS )
        {
            if ( name.matches( bannedPattern ) || fname.matches( bannedPattern ) )
            {
                return false;
            }
        }

        return true;
    }

    public static String makePath( final String[] parts, final int startingPos )
    {
        final String[] pathParts = new String[parts.length - startingPos];
        System.arraycopy( parts, startingPos, pathParts, 0, pathParts.length );
        return join( pathParts, "/" );
    }

    public static String appendPath( final String base, final String... parts )
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( base );
        for ( final String part : parts )
        {
            if ( sb.charAt( sb.length() - 1 ) == '/' )
            {
                sb.setLength( sb.length() - 1 );
            }

            if ( !part.startsWith( "/" ) )
            {
                sb.append( "/" );
            }

            sb.append( part );
        }

        return sb.toString();
    }

    public static String trimLeadingSlash( final String childName )
    {
        String name = childName;
        if ( name.startsWith( "/" ) )
        {
            if ( name.length() > 1 )
            {
                name = name.substring( 1 );
            }
            else
            {
                name = null;
            }
        }

        return name;
    }

    public static String formatSettingsResourceName( final StoreKey key )
    {
        return formatSettingsResourceName( key.getType(), key.getName() );
    }

    public static String formatSettingsResourceName( final StoreType type, final String name )
    {
        return "settings-" + name + ".xml";
    }

}
