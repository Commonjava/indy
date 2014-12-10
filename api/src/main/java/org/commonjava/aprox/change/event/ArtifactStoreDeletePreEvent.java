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
 * Event signaling the deletion of one or more {@link ArtifactStore} instances is ABOUT TO HAPPEN. This event will always contain a mapping of 
 * affected stores to their root storage locations, available via {@link #getStoreRoots()}.
 */
public class ArtifactStoreDeletePreEvent
    extends AbstractStoreDeleteEvent
{

    public ArtifactStoreDeletePreEvent( final Map<ArtifactStore, Transfer> storeRoots )
    {
        super( storeRoots );
    }

}
