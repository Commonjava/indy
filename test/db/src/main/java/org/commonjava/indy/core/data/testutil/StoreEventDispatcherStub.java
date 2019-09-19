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
package org.commonjava.indy.core.data.testutil;

import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.data.StoreEventDispatcher;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.maven.galley.event.EventMetadata;

import java.util.Map;

public class StoreEventDispatcherStub
    implements StoreEventDispatcher
{

    @Override
    public void deleting( final EventMetadata metadata, final ArtifactStore... stores )
    {
    }

    @Override
    public void deleted( final EventMetadata metadata, final ArtifactStore... stores )
    {
    }

    @Override
    public void updating( final ArtifactStoreUpdateType type, final EventMetadata metadata,
                          final Map<ArtifactStore, ArtifactStore> stores )
    {
    }

    @Override
    public void updated( final ArtifactStoreUpdateType type, final EventMetadata metadata,
                         final Map<ArtifactStore, ArtifactStore> stores )
    {
    }

    @Override
    public void enabling( EventMetadata eventMetadata, ArtifactStore... stores )
    {

    }

    @Override
    public void enabled( EventMetadata eventMetadata, ArtifactStore... stores )
    {

    }

    @Override
    public void disabling( EventMetadata eventMetadata, ArtifactStore... stores )
    {

    }

    @Override
    public void disabled( EventMetadata eventMetadata, ArtifactStore... stores )
    {

    }

}
