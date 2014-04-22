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
package org.commonjava.aprox.util;

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
