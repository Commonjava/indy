package org.commonjava.aprox.data;

import javax.enterprise.event.Event;

import org.commonjava.aprox.change.event.ArtifactStoreUpdateType;
import org.commonjava.aprox.model.core.ArtifactStore;

/**
 * Convenience component that standardizes the process of interacting with JEE {@link Event}s relating to changes in {@link ArtifactStore} definitions.
 */
public interface StoreEventDispatcher
{

    void deleting( final ArtifactStore... stores );

    void deleted( final ArtifactStore... stores );

    void updating( final ArtifactStoreUpdateType type, final ArtifactStore... stores );
}
