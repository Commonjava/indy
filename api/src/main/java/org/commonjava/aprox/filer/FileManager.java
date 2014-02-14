/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.filer;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.util.ArtifactPathInfo;
import org.commonjava.maven.galley.model.ConcreteResource;
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

    Transfer store( final ArtifactStore store, final String path, final InputStream stream )
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

    List<ConcreteResource> list( ArtifactStore store, String path )
        throws AproxWorkflowException;

    List<ConcreteResource> list( List<? extends ArtifactStore> stores, String path )
        throws AproxWorkflowException;

}
