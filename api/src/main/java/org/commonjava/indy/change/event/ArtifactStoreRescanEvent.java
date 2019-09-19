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
package org.commonjava.indy.change.event;

import java.util.Collection;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.maven.galley.event.EventMetadata;

/**
 * Event to signal that the rescanning of a particular artifact store has started.
 */
public class ArtifactStoreRescanEvent
    extends AbstractIndyEvent
{

    public ArtifactStoreRescanEvent( final EventMetadata eventMetadata, final ArtifactStore store )
    {
        super( eventMetadata, store );
    }

    public ArtifactStoreRescanEvent( final EventMetadata eventMetadata, final Collection<ArtifactStore> stores )
    {
        super( eventMetadata, stores );
    }

}
