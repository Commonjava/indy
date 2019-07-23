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

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.maven.galley.event.EventMetadata;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Created by jdcasey on 5/2/16.
 */
public class ArtifactStoreEnablementEvent
    implements IndyStoreEvent
{

    private final EventMetadata eventMetadata;

    private final boolean disabling;

    private Set<ArtifactStore> stores;

    private final boolean preprocessing;

    public ArtifactStoreEnablementEvent( boolean preprocessing, EventMetadata eventMetadata, boolean disabling, ArtifactStore... stores )
    {
        this.preprocessing = preprocessing;
        this.eventMetadata = eventMetadata;
        this.disabling = disabling;
        this.stores = new HashSet<>( Arrays.asList( stores ) );
    }

    public EventMetadata getEventMetadata()
    {
        return eventMetadata;
    }

    public boolean isDisabling()
    {
        return disabling;
    }

    public boolean isPreprocessing()
    {
        return preprocessing;
    }

    public Set<ArtifactStore> getStores()
    {
        return stores;
    }

    @Override
    public Iterator<ArtifactStore> iterator()
    {
        return stores.iterator();
    }

    @Override
    public void forEach( Consumer<? super ArtifactStore> action )
    {
        stores.forEach( action::accept );
    }

    @Override
    public Spliterator<ArtifactStore> spliterator()
    {
        return stores.spliterator();
    }

    @Override
    public String toString()
    {
        return "ArtifactStoreEnablementEvent{" +
                "eventMetadata=" + eventMetadata +
                ", disabling=" + disabling +
                ", stores=" + stores +
                ", preprocessing=" + preprocessing +
                '}';
    }
}
