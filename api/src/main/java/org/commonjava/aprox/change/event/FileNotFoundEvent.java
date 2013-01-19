package org.commonjava.aprox.change.event;

import java.util.Collections;
import java.util.List;

import org.commonjava.aprox.model.ArtifactStore;

public class FileNotFoundEvent
{

    private final List<? extends ArtifactStore> stores;

    private final String path;

    public FileNotFoundEvent( final List<? extends ArtifactStore> stores, final String path )
    {
        this.stores = stores;
        this.path = path;
    }

    public FileNotFoundEvent( final ArtifactStore store, final String path )
    {
        this.stores = Collections.singletonList( store );
        this.path = path;
    }

    public List<? extends ArtifactStore> getStores()
    {
        return stores;
    }

    public String getPath()
    {
        return path;
    }

}
