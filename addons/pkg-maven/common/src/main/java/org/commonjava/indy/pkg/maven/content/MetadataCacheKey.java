package org.commonjava.indy.pkg.maven.content;

import org.commonjava.indy.model.core.StoreKey;

import java.io.Serializable;
import java.util.Objects;

public final class MetadataCacheKey
                implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final StoreKey storeKey;

    private final String path;

    public MetadataCacheKey( StoreKey storeKey, String path )
    {
        this.storeKey = storeKey;
        this.path = path;
    }

    public StoreKey getStoreKey()
    {
        return storeKey;
    }

    public String getPath()
    {
        return path;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;
        if ( o == null || getClass() != o.getClass() )
            return false;
        MetadataCacheKey that = (MetadataCacheKey) o;
        return Objects.equals( storeKey, that.storeKey ) && Objects.equals( path, that.path );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( storeKey, path );
    }

    private static final String DELIMIT = "#";

    @Override
    public String toString()
    {
        return storeKey.toString() + DELIMIT + path; // e.g., maven:remote:central#/foo/bar/1.0/bar-1.0.pom
    }

    public static MetadataCacheKey fromString( String str )
    {
        int idx = str.lastIndexOf( DELIMIT );
        String storekey = str.substring( 0, idx );
        String path = str.substring( idx + 1 );
        return new MetadataCacheKey( StoreKey.fromString( storekey ), path );
    }
}
