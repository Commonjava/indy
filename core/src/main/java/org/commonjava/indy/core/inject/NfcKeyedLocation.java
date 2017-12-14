package org.commonjava.indy.core.inject;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.galley.KeyedLocation;

import java.util.Map;

/**
 * Created by ruhan on 11/29/17.
 */
public class NfcKeyedLocation implements KeyedLocation
{
    private StoreKey storeKey;

    public NfcKeyedLocation( StoreKey storeKey )
    {
        this.storeKey = storeKey;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;
        if ( o == null || getClass() != o.getClass() )
            return false;

        NfcKeyedLocation that = (NfcKeyedLocation) o;

        return storeKey.equals( that.storeKey );

    }

    @Override
    public int hashCode()
    {
        return storeKey.hashCode();
    }

    @Override
    public StoreKey getKey()
    {
        return storeKey;
    }

    @Override
    public boolean allowsDownloading()
    {
        return false;
    }

    @Override
    public boolean allowsPublishing()
    {
        return false;
    }

    @Override
    public boolean allowsStoring()
    {
        return false;
    }

    @Override
    public boolean allowsSnapshots()
    {
        return false;
    }

    @Override
    public boolean allowsReleases()
    {
        return false;
    }

    @Override
    public boolean allowsDeletion()
    {
        return false;
    }

    @Override
    public String getUri()
    {
        return null;
    }

    @Override
    public String getName()
    {
        return null;
    }

    @Override
    public Map<String, Object> getAttributes()
    {
        return null;
    }

    @Override
    public <T> T getAttribute( String s, Class<T> aClass )
    {
        return null;
    }

    @Override
    public <T> T getAttribute( String s, Class<T> aClass, T t )
    {
        return null;
    }

    @Override
    public Object removeAttribute( String s )
    {
        return null;
    }

    @Override
    public Object setAttribute( String s, Object o )
    {
        return null;
    }
}
