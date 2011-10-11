package org.commonjava.web.maven.proxy.model;

import org.commonjava.couch.model.AbstractCouchDocument;
import org.commonjava.couch.model.DenormalizedCouchDoc;

import com.google.gson.annotations.Expose;

public abstract class AbstractArtifactStore
    extends AbstractCouchDocument
    implements DenormalizedCouchDoc, ArtifactStore
{

    private String name;

    private StoreKey key;

    @Expose( deserialize = false )
    private final StoreType doctype;

    protected AbstractArtifactStore( final StoreType doctype )
    {
        this.doctype = doctype;
    }

    protected AbstractArtifactStore( final StoreType doctype, final String name )
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
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
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
        AbstractArtifactStore other = (AbstractArtifactStore) obj;
        if ( name == null )
        {
            if ( other.name != null )
            {
                return false;
            }
        }
        else if ( !name.equals( other.name ) )
        {
            return false;
        }
        return true;
    }

}