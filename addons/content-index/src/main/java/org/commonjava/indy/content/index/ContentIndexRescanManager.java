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
package org.commonjava.indy.content.index;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.change.event.ArtifactStorePostRescanEvent;
import org.commonjava.indy.change.event.ArtifactStorePreRescanEvent;

/**
 * A ContentIndexRescanManager is used to handle all related things with content index
 * for a store during the rescan processing of this store.
 */
public interface ContentIndexRescanManager
{
    void indexPreRescan( final ArtifactStorePreRescanEvent e )
            throws IndyWorkflowException;

    void indexPostRescan( final ArtifactStorePostRescanEvent e )
            throws IndyWorkflowException;
}
