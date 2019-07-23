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
import java.util.Map;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.maven.galley.event.EventMetadata;

/**
 * Event signaling that one or more specified {@link ArtifactStore} instances' configurations were changed. The {@link ArtifactStoreUpdateType}
 * gives more information about the nature of the update.
 * <br/>
 * This event is fired <b>AFTER</b> the updated {@link ArtifactStore} is actually persisted.
 * <br/>
 * As opposed to the {@link ArtifactStorePostUpdateEvent}, this one MAY run asynchronously to avoid performance penalties for the user..
 */
public class ArtifactStorePostUpdateEvent
    extends ArtifactStoreUpdateEvent
{

    public ArtifactStorePostUpdateEvent( final ArtifactStoreUpdateType type, final EventMetadata eventMetadata,
                                         final Map<ArtifactStore, ArtifactStore> changeMap )
    {
        super( type, eventMetadata, changeMap );
    }
}
