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
package org.commonjava.aprox.dto;

import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.util.UriFormatter;

import com.google.gson.annotations.SerializedName;

public final class EndpointView
    implements Comparable<EndpointView>
{
    private final String name;

    private final String type;

    @SerializedName( "resource_uri" )
    private final String resourceUri;

    public EndpointView( final ArtifactStore store, final String baseUri, final UriFormatter uriBuilder )
    {
        this.name = store.getName();

        this.type = store.getDoctype()
                         .name();

        this.resourceUri = uriBuilder.formatAbsolutePathTo( baseUri, store.getDoctype()
                                                                          .singularEndpointName(), store.getName() );

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
