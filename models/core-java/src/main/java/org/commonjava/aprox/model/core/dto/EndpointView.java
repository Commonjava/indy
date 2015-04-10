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
package org.commonjava.aprox.model.core.dto;

import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lightweight view of an {@link ArtifactStore} designed for external use. Each instance includes the name and type of the store, plus a resource-uri
 * for accessing content on that endpoint.
 */
public final class EndpointView
    implements Comparable<EndpointView>
{
    private String name;

    private String type;

    @JsonProperty( "resource_uri" )
    private String resourceUri;

    public EndpointView()
    {
    }

    public EndpointView( final ArtifactStore store, final String resourceUri )
    {
        final StoreKey key = store.getKey();
        this.name = key.getName();

        this.type = key.getType()
                       .name();


        this.resourceUri = resourceUri;
    }

    public final String getName()
    {
        return name;
    }

    public final String getType()
    {
        return type;
    }

    public final String getResourceUri()
    {
        return resourceUri;
    }

    public StoreType getStoreType()
    {
        return StoreType.get( type );
    }

    public StoreKey getStoreKey()
    {
        return new StoreKey( StoreType.get( type ), name );
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

    public void setResourceUri( final String resourceUri )
    {
        this.resourceUri = resourceUri;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public void setType( final String type )
    {
        this.type = type;
    }
}
