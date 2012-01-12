/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.core.change.event;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.commonjava.aprox.core.model.ArtifactStore;

public class ArtifactStoreUpdateEvent
    implements Iterable<ArtifactStore>
{

    private final ProxyManagerUpdateType type;

    private final Collection<ArtifactStore> changes;

    public ArtifactStoreUpdateEvent( final ProxyManagerUpdateType type, final Collection<ArtifactStore> changes )
    {
        this.type = type;
        this.changes = Collections.unmodifiableCollection( changes );
    }

    public ArtifactStoreUpdateEvent( final ProxyManagerUpdateType type, final ArtifactStore... changes )
    {
        this.changes = Collections.unmodifiableCollection( Arrays.asList( changes ) );
        this.type = type;
    }

    public ProxyManagerUpdateType getType()
    {
        return type;
    }

    @Override
    public Iterator<ArtifactStore> iterator()
    {
        return changes.iterator();
    }

    public Collection<ArtifactStore> getChanges()
    {
        return changes;
    }

}
