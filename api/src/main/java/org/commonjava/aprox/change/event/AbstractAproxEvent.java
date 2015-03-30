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

import org.commonjava.aprox.model.core.ArtifactStore;

/**
 * Base class for events related to changes in AProx {@link ArtifactStore} definitions.
 */
public class AbstractAproxEvent
    implements AproxStoreEvent
{

    protected final Collection<ArtifactStore> stores;

    protected AbstractAproxEvent( final Collection<ArtifactStore> stores )
    {
        this.stores = stores == null ? Collections.<ArtifactStore> emptySet() : clearNulls( stores );
    }

    private Collection<ArtifactStore> clearNulls( final Collection<ArtifactStore> stores )
    {
        for ( final Iterator<ArtifactStore> it = stores.iterator(); it.hasNext(); )
        {
            final ArtifactStore store = it.next();
            if ( store == null )
            {
                it.remove();
            }
        }

        return stores;
    }

    protected AbstractAproxEvent( final ArtifactStore... stores )
    {
        this.stores =
            stores == null || stores.length == 0 ? Collections.<ArtifactStore> emptySet() : Arrays.asList( stores );
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
