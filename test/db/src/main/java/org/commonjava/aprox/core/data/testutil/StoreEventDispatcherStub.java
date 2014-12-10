package org.commonjava.aprox.core.data.testutil;

import org.commonjava.aprox.change.event.ArtifactStoreUpdateType;
import org.commonjava.aprox.data.StoreEventDispatcher;
import org.commonjava.aprox.model.core.ArtifactStore;

public class StoreEventDispatcherStub
    implements StoreEventDispatcher
{

    @Override
    public void deleting( final ArtifactStore... stores )
    {
    }

    @Override
    public void deleted( final ArtifactStore... stores )
    {
    }

    @Override
    public void updating( final ArtifactStoreUpdateType type, final ArtifactStore... stores )
    {
    }

}
