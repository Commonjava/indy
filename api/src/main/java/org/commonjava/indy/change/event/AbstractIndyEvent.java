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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.maven.galley.event.EventMetadata;

/**
 * Base class for events related to changes in Indy {@link ArtifactStore} definitions.
 */
public class AbstractIndyEvent
    implements IndyStoreEvent
{

    protected final Collection<ArtifactStore> stores;

    private final EventMetadata eventMetadata;

    protected AbstractIndyEvent( final EventMetadata eventMetadata, final Collection<ArtifactStore> stores )
    {
        this.eventMetadata = eventMetadata;
        this.stores = stores == null ? Collections.emptySet() : clearNulls( stores );
    }

    protected AbstractIndyEvent( final EventMetadata eventMetadata, final ArtifactStore... stores )
    {
        this.eventMetadata = eventMetadata;

        this.stores = stores == null || stores.length == 0 ?
                Collections.emptySet() :
                clearNulls( Arrays.asList( stores ) );
    }

    public static Collection<ArtifactStore> clearNulls( final Collection<ArtifactStore> stores )
    {
        return stores.stream().filter( (store)-> store != null ).collect( Collectors.toSet() );
    }

    public final EventMetadata getEventMetadata()
    {
        return eventMetadata;
    }

    public final Collection<ArtifactStore> getStores()
    {
        return stores;
    }

    @Override
    public final Iterator<ArtifactStore> iterator()
    {
        return stores.iterator();
    }

}
