package org.commonjava.aprox.change.event;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.maven.galley.model.Transfer;

public class AbstractStoreDeleteEvent
    implements AproxStoreEvent
{

    protected final Map<ArtifactStore, Transfer> storeRoots;

    public AbstractStoreDeleteEvent( final Map<ArtifactStore, Transfer> storeRoots )
    {
        this.storeRoots = storeRoots == null ? Collections.<ArtifactStore, Transfer> emptyMap() : storeRoots;
    }

    @Override
    public String toString()
    {
        return String.format( "%s [storeRoots=%s]", getClass().getSimpleName(), storeRoots );
    }

    public Map<ArtifactStore, Transfer> getStoreRoots()
    {
        return storeRoots;
    }

    @Override
    public Iterator<ArtifactStore> iterator()
    {
        return storeRoots.keySet()
                         .iterator();
    }

}