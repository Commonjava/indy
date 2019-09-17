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

import java.io.InputStream;
import java.util.List;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;

/**
 * Organizes resolution of content from cached locations, remote locations, and group membership. This is a low-level interface intended to be used by
 * {@link ContentManager} and {@link ContentGenerator} implementations. It does not include results from generator sources (i.e. ContentProducer).
 */
public interface DownloadManager
{

    /**
     * Key used to bind {@link RemoteRepository} instances to the HTTP client in order to extract SSL/authentication info.
     * <b>TODO:</b> Given the attribute methods in the Galley {@link Location} api, this <b>should</b> be obsolete.
     */
    String HTTP_PARAM_REPO = "repository";

    /**
     * Root path used when retrieving the root of a {@link Location}'s cache.
     */
    String ROOT_PATH = "/";

    /**
     * Retrieve the content at the given path from the first store possible, then return then return the transfer that references the content without 
     * iterating any farther.
     */
    Transfer retrieveFirst( final List<? extends ArtifactStore> stores, final String path )
        throws IndyWorkflowException;

    /**
     * Retrieve the content at the given path from the first store possible, then return then return the transfer that references the content without 
     * iterating any farther.
     * @param eventMetadata TODO
     */
    Transfer retrieveFirst( final List<? extends ArtifactStore> stores , final String path , EventMetadata eventMetadata  )
        throws IndyWorkflowException;

    /**
     * Retrieve the content at the given path from all stores, then return the set of transfers that reference the content.
     */
    List<Transfer> retrieveAll( final List<? extends ArtifactStore> stores, final String path )
        throws IndyWorkflowException;

    /**
     * Retrieve the content at the given path from all stores, then return the set of transfers that reference the content.
     * @param eventMetadata TODO
     */
    List<Transfer> retrieveAll( final List<? extends ArtifactStore> stores , final String path , EventMetadata eventMetadata  )
        throws IndyWorkflowException;

    /**
     * Retrieve the content at the given path from the given store, then return the transfer that references the content.
     */
    Transfer retrieve( final ArtifactStore store, final String path )
        throws IndyWorkflowException;

    /**
     * Retrieve the content at the given path from the given store, then return the transfer that references the content.
     * @param eventMetadata TODO
     */
    Transfer retrieve( final ArtifactStore store , final String path , EventMetadata eventMetadata  )
        throws IndyWorkflowException;

    /**
     * Store the content contained in the {@link InputStream} under the given path within the storage directory for the given {@link ArtifactStore}.
     * Use the given {@link TransferOperation} to trigger the appropriate tangential maintenance, etc. actions. Return the {@link Transfer} that 
     * references the stored content.
     * <br/>
     * If the given {@link ArtifactStore} isn't a {@link HostedRepository}, or is a {@link Group} that doesn't contain a {@link HostedRepository}, 
     * fail. If the {@link HostedRepository} instances involved don't allow deployment/storage, or don't allow <b>appropriate</b> deployment 
     * (releases-only for snapshot content, or vice versa), then fail.
     */
    Transfer store( final ArtifactStore store, final String path, final InputStream stream, TransferOperation op )
        throws IndyWorkflowException;

    /**
     * Store the content contained in the {@link InputStream} under the given path within the storage directory for the given {@link ArtifactStore}.
     * Use the given {@link TransferOperation} to trigger the appropriate tangential maintenance, etc. actions. Return the {@link Transfer} that 
     * references the stored content.
     * <br/>
     * If the given {@link ArtifactStore} isn't a {@link HostedRepository}, or is a {@link Group} that doesn't contain a {@link HostedRepository}, 
     * fail. If the {@link HostedRepository} instances involved don't allow deployment/storage, or don't allow <b>appropriate</b> deployment 
     * (releases-only for snapshot content, or vice versa), then fail.
     * @param eventMetadata TODO
     */
    Transfer store( final ArtifactStore store , final String path , final InputStream stream , TransferOperation op , EventMetadata eventMetadata  )
        throws IndyWorkflowException;

    /**
     * Store the content contained in the {@link InputStream} under the given path within the storage directory for first appropriate instance among 
     * the given {@link ArtifactStore}'s. Use the given {@link TransferOperation} to trigger the appropriate tangential maintenance, etc. actions. 
     * Return the {@link Transfer} that references the stored content.
     * <br/>
     * If the given {@link ArtifactStore} isn't a {@link HostedRepository}, or is a {@link Group} that doesn't contain a {@link HostedRepository}, 
     * fail. If the {@link HostedRepository} instances involved don't allow deployment/storage, or don't allow <b>appropriate</b> deployment 
     * (releases-only for snapshot content, or vice versa), then fail.
     */
    Transfer store( final List<? extends ArtifactStore> stores, final String path, final InputStream stream,
                    TransferOperation op )
        throws IndyWorkflowException;

    /**
     * Store the content contained in the {@link InputStream} under the given path within the storage directory for first appropriate instance among 
     * the given {@link ArtifactStore}'s. Use the given {@link TransferOperation} to trigger the appropriate tangential maintenance, etc. actions. 
     * Return the {@link Transfer} that references the stored content.
     * <br/>
     * If the given {@link ArtifactStore} isn't a {@link HostedRepository}, or is a {@link Group} that doesn't contain a {@link HostedRepository}, 
     * fail. If the {@link HostedRepository} instances involved don't allow deployment/storage, or don't allow <b>appropriate</b> deployment 
     * (releases-only for snapshot content, or vice versa), then fail.
     * @param eventMetadata TODO
     */
    Transfer store( final List<? extends ArtifactStore> stores , final String path , final InputStream stream ,
                    TransferOperation op , EventMetadata eventMetadata  )
        throws IndyWorkflowException;

    boolean delete( final ArtifactStore store, String path )
    throws IndyWorkflowException;

    boolean delete( final ArtifactStore store , String path , EventMetadata eventMetadata  )
        throws IndyWorkflowException;

    boolean deleteAll( final List<? extends ArtifactStore> stores, String path )
        throws IndyWorkflowException;

    void rescan( final ArtifactStore store )
    throws IndyWorkflowException;

    void rescan( final ArtifactStore store , EventMetadata eventMetadata  )
        throws IndyWorkflowException;

    void rescanAll( final List<? extends ArtifactStore> stores )
    throws IndyWorkflowException;

    void rescanAll( final List<? extends ArtifactStore> stores , EventMetadata eventMetadata  )
        throws IndyWorkflowException;

    List<StoreResource> list( ArtifactStore store, String path )
            throws IndyWorkflowException;

    List<StoreResource> list( ArtifactStore store, String path, EventMetadata eventMetadatada )
        throws IndyWorkflowException;

    List<StoreResource> list( List<? extends ArtifactStore> stores, String path )
            throws IndyWorkflowException;

    List<StoreResource> list( List<? extends ArtifactStore> stores, String path, EventMetadata eventMetadata )
        throws IndyWorkflowException;

    Transfer getStoreRootDirectory( StoreKey key )
        throws IndyWorkflowException;

    Transfer getStoreRootDirectory( ArtifactStore store );

    Transfer getStorageReference( final StoreKey key, final String... path )
        throws IndyWorkflowException;

    Transfer getStorageReference( final ArtifactStore store, final String... path );

    List<Transfer> listRecursively( StoreKey src, String startPath )
        throws IndyWorkflowException;

    /**
     * Retrieve a {@link Transfer} object suitable for use in the specified operation. This method handles the selection logic, and doesn't fire any 
     * events (the returned {@link Transfer} object handles that in this case). The first suitable store is used to hold the Transfer.
     * 
     * @param stores The stores in which the Transfer should reside
     * @param path The path of the Transfer inside the store
     * @param op The operation we want to execute on the returned Transfer
     * @return A suitable transfer object (not null; those cases result in an exception) NOTE: the returned transfer may not exist!
     * @throws IndyWorkflowException in case no suitable storage location can be found
     */
    Transfer getStorageReference( List<ArtifactStore> stores, String path, TransferOperation op )
        throws IndyWorkflowException;

    /**
     * Retrieve a {@link Transfer} object suitable for use in the specified operation. This method handles the selection logic in the event the store 
     * is a {@link Group}, and doesn't fire any events (the returned {@link Transfer} object handles that in this case).
     * 
     * @param store The store in which the Transfer should reside
     * @param path The path of the Transfer inside the store
     * @param op The operation we want to execute on the returned Transfer
     * @return A suitable transfer object (not null; those cases result in an exception) NOTE: the returned transfer may not exist!
     * @throws IndyWorkflowException in case no suitable storage location can be found
     */
    Transfer getStorageReference( ArtifactStore store, String path, TransferOperation op )
        throws IndyWorkflowException;

    /**
     * Artifact existence check for given store and path. This method doesn't fire any events.
     *
     * @param store The store in which the check is performed
     * @param path The path of the Transfer inside the store
     * @throws IndyWorkflowException in case no suitable storage location can be found
     */
    boolean exists( ArtifactStore store, String path )
        throws IndyWorkflowException;
}
