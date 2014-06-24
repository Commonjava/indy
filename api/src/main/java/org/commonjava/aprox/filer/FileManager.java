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
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;

public interface FileManager
{

    String HTTP_PARAM_REPO = "repository";

    String ROOT_PATH = "/";

    Transfer retrieveFirst( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException;

    Set<Transfer> retrieveAll( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException;

    Transfer retrieve( final ArtifactStore store, final String path )
        throws AproxWorkflowException;

    Transfer store( final ArtifactStore store, final String path, final InputStream stream, TransferOperation op )
        throws AproxWorkflowException;

    Transfer store( final List<? extends ArtifactStore> stores, final String path, final InputStream stream,
                    TransferOperation op )
        throws AproxWorkflowException;

    Transfer getStoreRootDirectory( StoreKey key )
        throws AproxWorkflowException;

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

    Transfer getStoreRootDirectory( ArtifactStore store );

}
