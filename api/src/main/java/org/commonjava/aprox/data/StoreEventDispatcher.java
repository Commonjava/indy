/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.data;

import javax.enterprise.event.Event;

import org.commonjava.aprox.change.event.ArtifactStoreUpdateType;
import org.commonjava.aprox.model.core.ArtifactStore;

/**
 * Convenience component that standardizes the process of interacting with JEE {@link Event}s relating to changes in {@link ArtifactStore} definitions.
 */
public interface StoreEventDispatcher
{

    void deleting( final ArtifactStore... stores );

    void deleted( final ArtifactStore... stores );

    void updating( final ArtifactStoreUpdateType type, final ArtifactStore... stores );

    void updated( final ArtifactStoreUpdateType type, final ArtifactStore... stores );
}
