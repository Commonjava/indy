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

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import java.util.List;
import java.util.Map;

/**
 * This component is useful for content generators, that need access to raw files used as inputs for generating other
 * files. For instance, the maven-metadata.xml files from each constituent repository in a group are used to generate
 * the merged file. However, since we don't want to end up in an infinite recursion, we don't want to call back into
 * the {@link ContentManager} (which decorates its content accesses).
 *
 * Created by jdcasey on 5/2/16.
 */
public interface DirectContentAccess
{

    /**
     * Mainly useful for content generators, this provides a mechanism for bypassing the content generation aspects
     * of the content manager and access whatever concrete content there is. This method will NOT expand a {@link Group}
     * to retrieve content from its members; instead, it's up to the caller to detect this scenario and call
     * {@link #retrieveAllRaw(List, String, EventMetadata)} instead.
     */
    Transfer retrieveRaw( final ArtifactStore store, final String path, EventMetadata eventMetadata )
            throws IndyWorkflowException;

    /**
     * Mainly useful for content generators, this provides a mechanism for bypassing the content generation aspects
     * of the content manager and access whatever concrete content there is.
     */
    List<Transfer> retrieveAllRaw( final List<? extends ArtifactStore> stores, final String path, EventMetadata eventMetadata )
            throws IndyWorkflowException;

    /**
     * Retrieve a {@link Transfer} object suitable for use in the specified operation.
     *
     * @param store The store in which the Transfer should reside
     * @param path The path of the Transfer inside the store
     * @return A suitable transfer object (not null; those cases result in an exception) NOTE: the returned transfer may not exist!
     */
    Transfer getTransfer( final ArtifactStore store, final String path )
            throws IndyWorkflowException;

    /**
     * Retrieve a {@link Transfer} object suitable for use in the specified operation. This method handles the selection logic in the event the store
     * is a {@link Group}, and doesn't fire any events (the returned {@link Transfer} object handles that in this case).<br/>
     * <b>No content generators are called in this method.</b>
     *
     * @param storeKey The key to the store in which the Transfer should reside
     * @param path The path of the Transfer inside the store
     * @return A suitable transfer object (not null; those cases result in an exception) NOTE: the returned transfer may not exist!
     * @throws IndyWorkflowException in case no suitable storage location can be found
     */
    Transfer getTransfer( StoreKey storeKey, String path )
            throws IndyWorkflowException;

    boolean exists( ArtifactStore store, String path )
            throws IndyWorkflowException;

    List<StoreResource> listRaw( ArtifactStore store, String parentPath )
            throws IndyWorkflowException;

    List<StoreResource> listRaw( ArtifactStore store, String parentPath, EventMetadata eventMetadata )
            throws IndyWorkflowException;

    Map<String, List<StoreResource>> listRaw( ArtifactStore store, List<String> parentPathList )
            throws IndyWorkflowException;

    Map<String, List<StoreResource>> listRaw( ArtifactStore store, List<String> parentPathList, EventMetadata eventMetadata )
            throws IndyWorkflowException;

}
