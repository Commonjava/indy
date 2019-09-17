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
package org.commonjava.indy.util;


import java.io.File;

import static org.commonjava.maven.galley.util.PathUtils.normalize;
import static org.commonjava.maven.galley.util.PathUtils.parentPath;

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

    public static String removeExtraSlash( String path )
    {
        return path.replaceAll( "/+", "/" );
    }

    public static String getCurrentDirPath( String path )
    {
        if ( path.trim().endsWith( "/" ) )
        {
            return path;
        }
        return normalize( normalize( parentPath( path ) ), "/" );
    }
}
