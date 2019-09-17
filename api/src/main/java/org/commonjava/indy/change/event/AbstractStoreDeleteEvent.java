/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.change.event;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;

/**
 * Abstract event triggered when one or more {@link ArtifactStore} definitions are deleted. Includes references to the root of their storage locations, 
 * to allow cleanup of cached/stored content.
 */
public abstract class AbstractStoreDeleteEvent
    implements IndyStoreEvent
{

    protected final Map<ArtifactStore, Transfer> storeRoots;

    private final EventMetadata eventMetadata;

    protected AbstractStoreDeleteEvent( final EventMetadata eventMetadata, final Map<ArtifactStore, Transfer> storeRoots )
    {
        this.eventMetadata = eventMetadata;
        this.storeRoots = storeRoots == null ? Collections.<ArtifactStore, Transfer> emptyMap() : storeRoots;
    }

    public final EventMetadata getEventMetadata()
    {
        return eventMetadata;
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
    public Set<ArtifactStore> getStores()
    {
        return storeRoots.keySet();
    }

    @Override
    public Iterator<ArtifactStore> iterator()
    {
        return storeRoots.keySet()
                         .iterator();
    }

}