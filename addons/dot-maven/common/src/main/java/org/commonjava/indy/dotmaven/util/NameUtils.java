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
package org.commonjava.indy.dotmaven.util;

import static org.apache.commons.lang.StringUtils.join;

import java.io.File;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;

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
