/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.kafka.event;

import org.commonjava.event.common.EventMetadata;
import org.commonjava.event.store.EventStoreKey;
import org.commonjava.event.store.IndyStoreEvent;
import org.commonjava.event.store.StoreEventType;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class DefualtIndyStoreEvent
                implements IndyStoreEvent
{

    private StoreEventType eventType;

    private Set<EventStoreKey> keys;

    private EventMetadata eventMetadata;

    @Override
    public StoreEventType getEventType()
    {
        return eventType;
    }

    @Override
    public Collection<EventStoreKey> getKeys()
    {
        return keys;
    }

    @Override
    public EventMetadata getEventMetadata()
    {
        return eventMetadata;
    }

    @Override
    public final Iterator<EventStoreKey> iterator()
    {
        return keys.iterator();
    }
}
