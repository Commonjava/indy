package org.commonjava.aprox.folo.model;

import org.commonjava.aprox.model.core.StoreKey;

public class TrackingKey
{

    private String id;

    private StoreKey trackedStore;

    protected TrackingKey()
    {
    }

    protected void setId( final String id )
    {
        if ( id == null )
        {
            throw new NullPointerException( "tracking id cannot be null." );
        }

        this.id = id;
    }

    protected void setTrackedStore( final StoreKey trackedStore )
    {
        if ( trackedStore == null )
        {
            throw new NullPointerException( "tracked store (StoreKey) cannot be null." );
        }

        this.trackedStore = trackedStore;
    }

    public TrackingKey( final String id, final StoreKey trackedStore )
    {
        setId( id );
        setTrackedStore( trackedStore );
    }

    public String getId()
    {
        return id;
    }

    public StoreKey getTrackedStore()
    {
        return trackedStore;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        result = prime * result + ( ( trackedStore == null ) ? 0 : trackedStore.hashCode() );
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
        final TrackingKey other = (TrackingKey) obj;
        if ( id == null )
        {
            if ( other.id != null )
            {
                return false;
            }
        }
        else if ( !id.equals( other.id ) )
        {
            return false;
        }
        if ( trackedStore == null )
        {
            if ( other.trackedStore != null )
            {
                return false;
            }
        }
        else if ( !trackedStore.equals( other.trackedStore ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "TrackingKey [id=%s, storeKey=%s]", id, trackedStore );
    }

}
