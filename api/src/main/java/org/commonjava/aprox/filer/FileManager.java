/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.filer;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;

/**
 * Organizes resolution of content from cached locations, remote locations, and group membership. All content access in the core of AProx flow through
 * implementations of this.
 */
public interface FileManager
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
        throws AproxWorkflowException;

    /**
     * Retrieve the content at the given path from all stores, then return the set of transfers that reference the content.
     */
    Set<Transfer> retrieveAll( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException;

    /**
     * Retrieve the content at the given path from the given store, then return the transfer that references the content.
     */
    Transfer retrieve( final ArtifactStore store, final String path )
        throws AproxWorkflowException;

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
        throws AproxWorkflowException;

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
        throws AproxWorkflowException;

    Transfer getStoreRootDirectory( StoreKey key )
        throws AproxWorkflowException;

    Transfer getStoreRootDirectory( ArtifactStore store );

    Transfer getStorageReference( final StoreKey key, final String... path )
        throws AproxWorkflowException;

    Transfer getStorageReference( final ArtifactStore store, final String... path );

    ArtifactPathInfo parsePathInfo( String path );

    boolean delete( final ArtifactStore store, String path )
        throws AproxWorkflowException;

    boolean deleteAll( final List<? extends ArtifactStore> stores, String path )
        throws AproxWorkflowException;

    void rescan( final ArtifactStore store )
        throws AproxWorkflowException;

    void rescanAll( final List<? extends ArtifactStore> stores )
        throws AproxWorkflowException;

    List<ConcreteResource> list( ArtifactStore store, String path )
        throws AproxWorkflowException;

    List<ConcreteResource> list( List<? extends ArtifactStore> stores, String path )
        throws AproxWorkflowException;

}
