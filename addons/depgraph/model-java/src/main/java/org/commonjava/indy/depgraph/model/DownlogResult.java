/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.depgraph.model;

import org.apache.commons.lang.StringUtils;

import java.util.Set;

public class DownlogResult
                implements PlainRenderable
{

    private String linePrefix;

    private Set<String> locations;

    public DownlogResult()
    {
    }

    public DownlogResult( String linePrefix, Set<String> locations )
    {
        this.linePrefix = linePrefix;
        this.locations = locations;
    }

    @Override
    public String render()
    {
        final StringBuilder sb = new StringBuilder();
        for ( final String location : locations )
        {
            if ( StringUtils.isEmpty( location ) )
            {
                continue;
            }

            if ( StringUtils.isNotEmpty( linePrefix ) )
            {
                sb.append( linePrefix );
            }

            sb.append( location ).append( "\n" );
        }

        return sb.toString();
    }


    public String getLinePrefix()
    {
        return linePrefix;
    }

    public void setLinePrefix( final String linePrefix )
    {
        this.linePrefix = linePrefix;
    }

    public Set<String> getLocations()
    {
        return locations;
    }

    public void setLocations( final Set<String> locations )
    {
        this.locations = locations;
    }
}
