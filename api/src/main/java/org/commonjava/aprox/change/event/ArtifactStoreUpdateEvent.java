/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.change.event;

import java.util.Arrays;
import java.util.Collection;

import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.maven.galley.event.EventMetadata;

/**
 * Event signaling that one or more specified {@link ArtifactStore} instances' configurations were changed. The {@link ArtifactStoreUpdateType}
 * gives more information about the nature of the update.
 */
public abstract class ArtifactStoreUpdateEvent
    extends AbstractAproxEvent
{

    private final ArtifactStoreUpdateType type;

    protected ArtifactStoreUpdateEvent( final ArtifactStoreUpdateType type, final EventMetadata eventMetadata,
                                        final Collection<ArtifactStore> changes )
    {
        super( eventMetadata, changes );
        this.type = type;
    }

    protected ArtifactStoreUpdateEvent( final ArtifactStoreUpdateType type, final EventMetadata eventMetadata,
                                        final ArtifactStore... changes )
    {
        super( eventMetadata, Arrays.asList( changes ) );
        this.type = type;
    }

    /**
     * Return the type of update that took place.
     */
    public ArtifactStoreUpdateType getType()
    {
        return type;
    }

    /**
     * Return the changed {@link ArtifactStore}'s specified in this event.
     */
    public Collection<ArtifactStore> getChanges()
    {
        return getStores();
    }

}
