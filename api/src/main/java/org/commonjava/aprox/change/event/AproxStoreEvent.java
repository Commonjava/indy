package org.commonjava.aprox.change.event;

import org.commonjava.aprox.model.core.ArtifactStore;

/**
 * Marker interface for events related to changes in {@link ArtifactStore} instances.
 */
public interface AproxStoreEvent
    extends Iterable<ArtifactStore>
{

}
