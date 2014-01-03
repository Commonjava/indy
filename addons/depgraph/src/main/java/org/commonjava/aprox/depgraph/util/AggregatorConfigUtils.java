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

import static org.apache.commons.io.IOUtils.readLines;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.commonjava.aprox.depgraph.json.JsonUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.agg.AggregatorConfig;

public final class AggregatorConfigUtils
{

    private AggregatorConfigUtils()
    {
    }

    public static AggregatorConfig read( final InputStream stream )
        throws IOException
    {
        final List<String> lines = readLines( stream );
        return read( lines );
    }

    public static AggregatorConfig read( final Reader reader )
        throws IOException
    {
        final List<String> lines = readLines( reader );
        return read( lines );
    }

    public static AggregatorConfig read( final List<String> lines )
    {
        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>();

        for ( final String line : lines )
        {
            if ( line.trim()
                     .length() < 1 || line.trim()
                                          .startsWith( "#" ) )
            {
                continue;
            }

            final ProjectVersionRef ref = JsonUtils.parseProjectVersionRef( line );
            if ( ref != null )
            {
                refs.add( ref );
            }
        }

        return new AggregatorConfig( refs );
    }

}
