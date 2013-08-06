/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.filer;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ArtifactPathInfo;
import org.commonjava.maven.galley.model.Transfer;

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

    Transfer store( final DeployPoint deploy, final String path, final InputStream stream )
        throws AproxWorkflowException;

    Transfer store( final List<? extends ArtifactStore> stores, final String path, final InputStream stream )
        throws AproxWorkflowException;

    Transfer getStoreRootDirectory( StoreKey key );

    Transfer getStorageReference( final StoreKey key, final String... path );

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

}
