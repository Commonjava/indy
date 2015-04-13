package org.commonjava.aprox.folo.model;

import java.util.Set;
import java.util.TreeSet;

import org.commonjava.aprox.model.core.StoreKey;

public class AffectedStoreRecord
{

    private StoreKey key;

    private Set<String> uploadedPaths;

    private Set<String> downloadedPaths;

    protected AffectedStoreRecord()
    {
    }

    protected void setKey( final StoreKey key )
    {
        this.key = key;
    }

    public AffectedStoreRecord( final StoreKey key )
    {
        this.key = key;
    }

    public StoreKey getKey()
    {
        return key;
    }

    public Set<String> getDownloadedPaths()
    {
        return downloadedPaths;
    }

    public Set<String> getUploadedPaths()
    {
        return uploadedPaths;
    }

    public synchronized void add( final String path, final StoreEffect type )
    {
        if ( path == null )
        {
            return;
        }

        if ( type == StoreEffect.DOWNLOAD )
        {
            if ( downloadedPaths == null )
            {
                downloadedPaths = new TreeSet<>();
            }

            downloadedPaths.add( path );
        }
        else
        {
            if ( uploadedPaths == null )
            {
                uploadedPaths = new TreeSet<>();
            }

            uploadedPaths.add( path );
        }
    }

}
