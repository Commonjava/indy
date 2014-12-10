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

import org.commonjava.aprox.model.core.ArtifactStore;

/**
 * Event signaling that one or more specified {@link ArtifactStore} instances' configurations were changed. The {@link ArtifactStoreUpdateType}
 * gives more information about the nature of the update.
 */
public class ArtifactStoreUpdateEvent
    extends AbstractAproxEvent
{

    private final ArtifactStoreUpdateType type;

    public ArtifactStoreUpdateEvent( final ArtifactStoreUpdateType type, final Collection<ArtifactStore> changes )
    {
        super( changes );
        this.type = type;
    }

    public ArtifactStoreUpdateEvent( final ArtifactStoreUpdateType type, final ArtifactStore... changes )
    {
        super( Arrays.asList( changes ) );
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
