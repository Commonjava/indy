/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.dotmaven.data;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.HostedRepository;

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
