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
package org.commonjava.aprox.core.rest.stats;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.UriBuilder;

import org.commonjava.aprox.core.rest.RESTApplication;
import org.commonjava.aprox.model.ArtifactStore;

import com.google.gson.annotations.SerializedName;

public final class EndpointView
    implements Comparable<EndpointView>
{
    private static final ApplicationPath APP_PATH = RESTApplication.class.getAnnotation( ApplicationPath.class );

    private final String name;

    private final String type;

    @SerializedName( "resource_uri" )
    private final String resourceUri;

    public EndpointView( final ArtifactStore store, final UriBuilder uriBuilder )
    {
        this.name = store.getName();

        this.type = store.getDoctype()
                         .name();

        this.resourceUri = uriBuilder.replacePath( "" )
                                     .path( APP_PATH.value() )
                                     .path( store.getDoctype()
                                                 .singularEndpointName() )
                                     .path( store.getName() )
                                     .build()
                                     .toString();
    }

    public final String getName()
    {
        return name;
    }

    public final String getType()
    {
        return type;
    }

    public final String getResourceURI()
    {
        return resourceUri;
    }

    public final String getKey()
    {
        return type + ":" + name;
    }

    @Override
    public int compareTo( final EndpointView point )
    {
        return getKey().compareTo( point.getKey() );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( resourceUri == null ) ? 0 : resourceUri.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final EndpointView other = (EndpointView) obj;
        if ( resourceUri == null )
        {
            if ( other.resourceUri != null )
            {
                return false;
            }
        }
        else if ( !resourceUri.equals( other.resourceUri ) )
        {
            return false;
        }
        return true;
    }
}
