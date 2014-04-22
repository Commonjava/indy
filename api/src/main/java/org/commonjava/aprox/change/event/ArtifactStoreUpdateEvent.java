/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
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
