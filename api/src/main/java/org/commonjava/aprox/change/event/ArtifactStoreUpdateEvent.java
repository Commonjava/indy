/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.change.event;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.commonjava.aprox.model.ArtifactStore;

public class ArtifactStoreUpdateEvent
    implements Iterable<ArtifactStore>, AproxEvent
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
