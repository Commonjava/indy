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

import java.util.Map;

import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.maven.galley.model.Transfer;

/**
 * Event signaling the deletion of one or more {@link ArtifactStore} instances is COMPLETE. This event will always contain the same type of store, when there is
 * more than one. Instance names are collected and available via getNames(), while the store type is available separately via the getType() method.
 */
public class ArtifactStoreDeletePostEvent
    extends AbstractStoreDeleteEvent
{

    public ArtifactStoreDeletePostEvent( final Map<ArtifactStore, Transfer> storeRoots )
    {
        super( storeRoots );
    }

}
