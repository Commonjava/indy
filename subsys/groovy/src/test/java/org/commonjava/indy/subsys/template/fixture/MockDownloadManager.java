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
package org.commonjava.indy.subsys.template.fixture;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;

import javax.enterprise.inject.Alternative;
import java.io.InputStream;
import java.util.List;

/**
 * Created by gli on 1/5/17.
 */
@Alternative
public class MockDownloadManager implements DownloadManager
{
    @Override
    public Transfer retrieveFirst( List<? extends ArtifactStore> stores, String path )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public Transfer retrieveFirst( List<? extends ArtifactStore> stores, String path, EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public List<Transfer> retrieveAll( List<? extends ArtifactStore> stores, String path )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public List<Transfer> retrieveAll( List<? extends ArtifactStore> stores, String path, EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public Transfer retrieve( ArtifactStore store, String path )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public Transfer retrieve( ArtifactStore store, String path, EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public Transfer store( ArtifactStore store, String path, InputStream stream, TransferOperation op )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public Transfer store( ArtifactStore store, String path, InputStream stream, TransferOperation op,
                           EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public Transfer store( List<? extends ArtifactStore> stores, String path, InputStream stream, TransferOperation op )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public Transfer store( List<? extends ArtifactStore> stores, String path, InputStream stream, TransferOperation op,
                           EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public boolean delete( ArtifactStore store, String path )
            throws IndyWorkflowException
    {
        return false;
    }

    @Override
    public boolean delete( ArtifactStore store, String path, EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        return false;
    }

    @Override
    public boolean deleteAll( List<? extends ArtifactStore> stores, String path )
            throws IndyWorkflowException
    {
        return false;
    }

    @Override
    public void rescan( ArtifactStore store )
            throws IndyWorkflowException
    {

    }

    @Override
    public void rescan( ArtifactStore store, EventMetadata eventMetadata )
            throws IndyWorkflowException
    {

    }

    @Override
    public void rescanAll( List<? extends ArtifactStore> stores )
            throws IndyWorkflowException
    {

    }

    @Override
    public void rescanAll( List<? extends ArtifactStore> stores, EventMetadata eventMetadata )
            throws IndyWorkflowException
    {

    }

    @Override
    public List<StoreResource> list( ArtifactStore store, String path )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public List<StoreResource> list( ArtifactStore store, String path, EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public List<StoreResource> list( List<? extends ArtifactStore> stores, String path )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public List<StoreResource> list( List<? extends ArtifactStore> stores, String path, EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public Transfer getStoreRootDirectory( StoreKey key )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public Transfer getStoreRootDirectory( ArtifactStore store )
    {
        return null;
    }

    @Override
    public Transfer getStorageReference( StoreKey key, String... path )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public Transfer getStorageReference( ArtifactStore store, String... path )
    {
        return null;
    }

    @Override
    public List<Transfer> listRecursively( StoreKey src, String startPath )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public Transfer getStorageReference( List<ArtifactStore> stores, String path, TransferOperation op )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public Transfer getStorageReference( ArtifactStore store, String path, TransferOperation op )
            throws IndyWorkflowException
    {
        return null;
    }

    @Override
    public boolean exists( ArtifactStore store, String path )
            throws IndyWorkflowException
    {
        return false;
    }
}
