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
package org.commonjava.aprox.promote.validate;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.content.ContentDigest;
import org.commonjava.aprox.content.ContentManager;
import org.commonjava.aprox.content.StoreResource;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.transport.htcli.model.HttpExchangeMetadata;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jdcasey on 9/11/15.
 */
public class PromotionValidationTools
{
    private ContentManager contentManager;

    private final StoreDataManager storeDataManager;

    public PromotionValidationTools( ContentManager manager, StoreDataManager storeDataManager )
    {
        contentManager = manager;
        this.storeDataManager = storeDataManager;
    }

    public Transfer getTransfer( List<ArtifactStore> stores, String path, TransferOperation op )
            throws AproxWorkflowException
    {
        return contentManager.getTransfer( stores, path, op );
    }

    public Transfer getTransfer( StoreKey storeKey, String path, TransferOperation op )
            throws AproxWorkflowException
    {
        return contentManager.getTransfer( storeKey, path, op );
    }

    public Transfer getTransfer( ArtifactStore store, String path, TransferOperation op )
            throws AproxWorkflowException
    {
        return contentManager.getTransfer( store, path, op );
    }

    public Transfer retrieve( ArtifactStore store, String path, EventMetadata eventMetadata )
            throws AproxWorkflowException
    {
        return contentManager.retrieve( store, path, eventMetadata );
    }

    public Transfer retrieve( ArtifactStore store, String path )
            throws AproxWorkflowException
    {
        return contentManager.retrieve( store, path );
    }

    public List<Transfer> retrieveAll( List<? extends ArtifactStore> stores, String path, EventMetadata eventMetadata )
            throws AproxWorkflowException
    {
        return contentManager.retrieveAll( stores, path, eventMetadata );
    }

    public List<Transfer> retrieveAll( List<? extends ArtifactStore> stores, String path )
            throws AproxWorkflowException
    {
        return contentManager.retrieveAll( stores, path );
    }

    public Transfer retrieveFirst( List<? extends ArtifactStore> stores, String path, EventMetadata eventMetadata )
            throws AproxWorkflowException
    {
        return contentManager.retrieveFirst( stores, path, eventMetadata );
    }

    public Transfer retrieveFirst( List<? extends ArtifactStore> stores, String path )
            throws AproxWorkflowException
    {
        return contentManager.retrieveFirst( stores, path );
    }

    public List<StoreResource> list( ArtifactStore store, String path )
            throws AproxWorkflowException
    {
        return contentManager.list( store, path );
    }

    public List<StoreResource> list( ArtifactStore store, String path, EventMetadata eventMetadata )
            throws AproxWorkflowException
    {
        return contentManager.list( store, path, eventMetadata );
    }

    public List<StoreResource> list( List<? extends ArtifactStore> stores, String path )
            throws AproxWorkflowException
    {
        return contentManager.list( stores, path );
    }

    public Map<ContentDigest, String> digest( StoreKey key, String path, ContentDigest... types )
            throws AproxWorkflowException
    {
        return contentManager.digest( key, path, types );
    }

    public HttpExchangeMetadata getHttpMetadata( Transfer txfr )
            throws AproxWorkflowException
    {
        return contentManager.getHttpMetadata( txfr );
    }

    public HttpExchangeMetadata getHttpMetadata( StoreKey storeKey, String path )
            throws AproxWorkflowException
    {
        return contentManager.getHttpMetadata( storeKey, path );
    }

    public HostedRepository getHostedRepository( String name )
            throws AproxDataException
    {
        return storeDataManager.getHostedRepository( name );
    }

    public RemoteRepository getRemoteRepository( String name )
            throws AproxDataException
    {
        return storeDataManager.getRemoteRepository( name );
    }

    public Group getGroup( String name )
            throws AproxDataException
    {
        return storeDataManager.getGroup( name );
    }

    public ArtifactStore getArtifactStore( StoreKey key )
            throws AproxDataException
    {
        return storeDataManager.getArtifactStore( key );
    }

    public List<ArtifactStore> getAllArtifactStores()
            throws AproxDataException
    {
        return storeDataManager.getAllArtifactStores();
    }

    public List<? extends ArtifactStore> getAllArtifactStores( StoreType type )
            throws AproxDataException
    {
        return storeDataManager.getAllArtifactStores( type );
    }

    public List<Group> getAllGroups()
            throws AproxDataException
    {
        return storeDataManager.getAllGroups();
    }

    public List<RemoteRepository> getAllRemoteRepositories()
            throws AproxDataException
    {
        return storeDataManager.getAllRemoteRepositories();
    }

    public List<HostedRepository> getAllHostedRepositories()
            throws AproxDataException
    {
        return storeDataManager.getAllHostedRepositories();
    }

    public List<ArtifactStore> getAllConcreteArtifactStores()
            throws AproxDataException
    {
        return storeDataManager.getAllConcreteArtifactStores();
    }

    public List<ArtifactStore> getOrderedConcreteStoresInGroup( String groupName )
            throws AproxDataException
    {
        return storeDataManager.getOrderedConcreteStoresInGroup( groupName );
    }

    public List<ArtifactStore> getOrderedStoresInGroup( String groupName )
            throws AproxDataException
    {
        return storeDataManager.getOrderedStoresInGroup( groupName );
    }

    public Set<Group> getGroupsContaining( StoreKey repo )
            throws AproxDataException
    {
        return storeDataManager.getGroupsContaining( repo );
    }

    public boolean hasRemoteRepository( String name )
    {
        return storeDataManager.hasRemoteRepository( name );
    }

    public boolean hasGroup( String name )
    {
        return storeDataManager.hasGroup( name );
    }

    public boolean hasHostedRepository( String name )
    {
        return storeDataManager.hasHostedRepository( name );
    }

    public boolean hasArtifactStore( StoreKey key )
    {
        return storeDataManager.hasArtifactStore( key );
    }

    public RemoteRepository findRemoteRepository( String url )
    {
        return storeDataManager.findRemoteRepository( url );
    }
}
