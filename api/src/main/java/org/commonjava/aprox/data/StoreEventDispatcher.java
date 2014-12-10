package org.commonjava.aprox.data;

import org.commonjava.aprox.change.event.ArtifactStoreUpdateType;
import org.commonjava.aprox.model.core.ArtifactStore;

public interface StoreEventDispatcher
{

    void deleting( final ArtifactStore... stores );

    void deleted( final ArtifactStore... stores );

    void updating( final ArtifactStoreUpdateType type, final ArtifactStore... stores );
}
