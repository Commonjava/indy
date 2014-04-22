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
package org.commonjava.aprox.dotmaven.data;

import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.HostedRepository;

public class StorageAdvice
{
    private final boolean deployable;

    private final boolean releasesAllowed;

    private final boolean snapshotsAllowed;

    private final ArtifactStore store;

    private final HostedRepository hostedStore;

    public StorageAdvice( final ArtifactStore store, final HostedRepository hostedStore, final boolean deployable, final boolean releasesAllowed,
                          final boolean snapshotsAllowed )
    {
        this.store = store;
        this.hostedStore = hostedStore;
        this.deployable = deployable;
        this.releasesAllowed = releasesAllowed;
        this.snapshotsAllowed = snapshotsAllowed;
    }

    public HostedRepository getHostedStore()
    {
        return hostedStore;
    }

    public ArtifactStore getStore()
    {
        return store;
    }

    public boolean isDeployable()
    {
        return deployable;
    }

    public boolean isReleasesAllowed()
    {
        return releasesAllowed;
    }

    public boolean isSnapshotsAllowed()
    {
        return snapshotsAllowed;
    }

}
