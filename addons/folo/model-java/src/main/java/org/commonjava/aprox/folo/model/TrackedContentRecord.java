package org.commonjava.aprox.folo.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.commonjava.aprox.model.core.StoreKey;

public class TrackedContentRecord
    implements Iterable<AffectedStoreRecord>
{

    private TrackingKey key;

    private Map<StoreKey, AffectedStoreRecord> affectedStores;

    protected TrackedContentRecord()
    {
    }

    protected void setKey( final TrackingKey key )
    {
        this.key = key;
    }

    public TrackedContentRecord( final TrackingKey key )
    {
        this.key = key;
    }

    public TrackingKey getKey()
    {
        return key;
    }

    public Map<StoreKey, AffectedStoreRecord> getAffectedStores()
    {
        return affectedStores;
    }

    public synchronized AffectedStoreRecord getAffectedStore( final StoreKey key, final boolean create )
    {
        if ( affectedStores == null )
        {
            if ( create )
            {
                affectedStores = new HashMap<>();
            }
            else
            {
                return null;
            }
        }

        AffectedStoreRecord store = affectedStores.get( key );
        if ( create && store == null )
        {
            store = new AffectedStoreRecord( key );
            affectedStores.put( key, store );
        }

        return store;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
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
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final TrackedContentRecord other = (TrackedContentRecord) obj;
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

    @Override
    public String toString()
    {
        return String.format( "TrackedStoreRecord [%s]", key );
    }

    @Override
    public Iterator<AffectedStoreRecord> iterator()
    {
        return affectedStores == null ? Collections.<AffectedStoreRecord> emptyIterator() : affectedStores.values()
                             .iterator();
    }

}
