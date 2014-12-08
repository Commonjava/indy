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
package org.commonjava.aprox.model.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class ArtifactStore
    implements Serializable
{

    public static final String METADATA_CHANGELOG = "changelog";

    private static final long serialVersionUID = 1L;

    private StoreKey key;

    private String description;

    private transient Map<String, String> transientMetadata;

    private Map<String, String> metadata;

    protected ArtifactStore()
    {
    }

    protected ArtifactStore( final String name )
    {
        this.key = initKey( name );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.web.maven.proxy.model.ArtifactStore#getName()
     */
    public String getName()
    {
        return key.getName();
    }

    protected void setName( final String name )
    {
        this.key = initKey( name );
    }

    @Deprecated
    public StoreType getDoctype()
    {
        return getKey().getType();
    }

    public synchronized StoreKey getKey()
    {
        return key;
    }

    protected abstract StoreKey initKey( String name );

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( key == null ) ? 0 : key.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final ArtifactStore other = (ArtifactStore) obj;
        if ( key == null )
        {
            if ( other.key != null )
            {
                return false;
            }
        }
        else if ( !key.equals( other.key ) )
        {
            return false;
        }
        return true;
    }

    public void setMetadata( final Map<String, String> metadata )
    {
        this.metadata = metadata;
    }

    public Map<String, String> getMetadata()
    {
        return metadata;
    }

    public synchronized String setMetadata( final String key, final String value )
    {
        if ( key == null || value == null )
        {
            return null;
        }

        if ( metadata == null )
        {
            metadata = new HashMap<String, String>();
        }

        return metadata.put( key, value );
    }

    public String getMetadata( final String key )
    {
        return metadata == null ? null : metadata.get( key );
    }

    public synchronized String setTransientMetadata( final String key, final String value )
    {
        if ( key == null || value == null )
        {
            return null;
        }

        if ( transientMetadata == null )
        {
            transientMetadata = new HashMap<String, String>();
        }

        return transientMetadata.put( key, value );
    }

    public String getTransientMetadata( final String key )
    {
        return transientMetadata == null ? null : transientMetadata.get( key );
    }

    @Override
    public String toString()
    {
        return String.format( "ArtifactStore [key=%s]", key );
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( final String description )
    {
        this.description = description;
    }

}
