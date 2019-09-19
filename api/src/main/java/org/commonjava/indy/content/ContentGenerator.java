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
package org.commonjava.indy.content;

import java.util.List;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;

/**
 * Interface to support dynamic content generation. This was originally intended for generating metadata files when they aren't present on
 * remote repositories. However, it's designed to accommodate all sorts of dynamic content.
 */
public interface ContentGenerator
{

    /**
     * Generate dynamic content in the event it's not found in the ArtifactStore. This is secondary to the main content retrieval logic, as a
     * last effort to avoid returning a missing result.
     */
    Transfer generateFileContent( ArtifactStore store, String path, EventMetadata eventMetadata )
        throws IndyWorkflowException;

    /**
     * Generate resources for any missing files that this generator can create. This is meant to contribute to an existing directory listing, so
     * the existing resources are given in order to allow the generator to determine whether a new resources is warranted.
     */
    List<StoreResource> generateDirectoryContent( ArtifactStore store, String path, List<StoreResource> existing,
                                                  EventMetadata eventMetadata )
        throws IndyWorkflowException;

    /**
     * Generate dynamic content for a group. This is the PRIMARY form of group access, with secondary action being to attempt normal retrieval from
     * one of the member stores.
     */
    Transfer generateGroupFileContent( Group group, List<ArtifactStore> members, String path,
                                       EventMetadata eventMetadata )
        throws IndyWorkflowException;

    /**
     * Generate resources for merged group files that this generator can create.
     */
    List<StoreResource> generateGroupDirectoryContent( Group group, List<ArtifactStore> members, String path,
                                                       EventMetadata eventMetadata )
        throws IndyWorkflowException;

    /**
     * Tidy up any generated content associated with the stored file
     */
    void handleContentStorage( ArtifactStore store, String path, Transfer result, EventMetadata eventMetadata )
        throws IndyWorkflowException;

    /**
     * Tidy up any generated content associated with the deleted file
     */
    void handleContentDeletion( ArtifactStore store, String path, EventMetadata eventMetadata )
        throws IndyWorkflowException;

    /**
     * Checks if this content generator processes the provided path.
     */
    boolean canProcess( String path );

}
