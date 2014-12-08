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

import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;

/**
 * Event to signal that the rescanning of a particular artifact store has started.
 */
public class ArtifactStoreRescanEvent
    implements AproxEvent
{

    private final StoreKey key;

    public ArtifactStoreRescanEvent( final StoreKey key )
    {
        this.key = key;
    }

    /**
     * Return the {@link StoreKey} for the {@link ArtifactStore} being rescanned.
     */
    public StoreKey getStoreKey()
    {
        return key;
    }

}
