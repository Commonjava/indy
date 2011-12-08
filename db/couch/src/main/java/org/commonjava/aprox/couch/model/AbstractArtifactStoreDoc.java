/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.couch.model;

import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.couch.model.AbstractCouchDocument;
import org.commonjava.couch.model.DenormalizedCouchDoc;

import com.google.gson.annotations.Expose;

public abstract class AbstractArtifactStoreDoc
    extends AbstractCouchDocument
    implements DenormalizedCouchDoc, ArtifactStore, ArtifactStoreDoc
{

    private String name;

    @Expose( serialize = false, deserialize = false )
    private StoreKey key;

    @Expose( deserialize = false )
    private final StoreType doctype;

    protected AbstractArtifactStoreDoc( final StoreType doctype )
    {
        this.doctype = doctype;
    }

    protected AbstractArtifactStoreDoc( final StoreType doctype, final String name )
    {
        this.doctype = doctype;
        this.name = name;
        this.key = new StoreKey( doctype, name );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.web.maven.proxy.model.ArtifactStore#getName()
     */
    @Override
    public String getName()
    {
        return name;
    }

    protected void setName( final String name )
    {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.web.maven.proxy.model.ArtifactStore#getDoctype()
     */
    @Override
    public StoreType getDoctype()
    {
        return doctype;
    }

    @Override
    public StoreKey getKey()
    {
        return key;
    }

    @Override
    public void calculateDenormalizedFields()
    {
        this.key = new StoreKey( doctype, name );
        setCouchDocId( key.toString() );
    }

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
        final AbstractArtifactStoreDoc other = (AbstractArtifactStoreDoc) obj;
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

}
