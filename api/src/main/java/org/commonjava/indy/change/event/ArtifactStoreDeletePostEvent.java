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

import java.util.Map;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;

/**
 * Event signaling the deletion of one or more {@link ArtifactStore} instances is COMPLETE. This event will always contain the same type of store, when there is
 * more than one. Instance names are collected and available via getNames(), while the store type is available separately via the getType() method.
 * <br/>
 * As opposed to the {@link ArtifactStoreDeletePreEvent}, this one MAY run asynchronously to avoid performance penalties for the user..
 */
public class ArtifactStoreDeletePostEvent
    extends AbstractStoreDeleteEvent
{

    public ArtifactStoreDeletePostEvent( final EventMetadata eventMetadata,
                                         final Map<ArtifactStore, Transfer> storeRoots )
    {
        super( eventMetadata, storeRoots );
    }

}
