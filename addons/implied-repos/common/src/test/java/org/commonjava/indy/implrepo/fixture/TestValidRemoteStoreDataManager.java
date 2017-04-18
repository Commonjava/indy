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
package org.commonjava.indy.implrepo.fixture;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.implrepo.data.ValidRemoteStoreDataManagerDecorator;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.event.EventMetadata;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class TestValidRemoteStoreDataManager
        extends ValidRemoteStoreDataManagerDecorator
        implements StoreDataManager
{

    private final StoreDataManager delegate;

    public TestValidRemoteStoreDataManager( final TransferManager transferManager )
    {
        super( new MemoryStoreDataManager( true ), transferManager );
        delegate = getDelegate();
    }

    @Override
    public HostedRepository getHostedRepository( String name )
            throws IndyDataException
    {
        return delegate.getHostedRepository( name );
    }

    @Override
    public RemoteRepository getRemoteRepository( String name )
            throws IndyDataException
    {
        return delegate.getRemoteRepository( name );
    }

    @Override
    public Group getGroup( String name )
            throws IndyDataException
    {
        return delegate.getGroup( name );
    }

    @Override
    public ArtifactStore getArtifactStore( StoreKey key )
            throws IndyDataException
    {
        return delegate.getArtifactStore( key );
    }

    @Override
    public List<ArtifactStore> getAllArtifactStores()
            throws IndyDataException
    {
        return delegate.getAllArtifactStores();
    }

    @Override
    public List<? extends ArtifactStore> getAllArtifactStores( StoreType type )
            throws IndyDataException
    {
        return delegate.getAllArtifactStores( type );
    }

    @Override
    public List<Group> getAllGroups()
            throws IndyDataException
    {
        return delegate.getAllGroups();
    }

    @Override
    public List<RemoteRepository> getAllRemoteRepositories()
            throws IndyDataException
    {
        return delegate.getAllRemoteRepositories();
    }

    @Override
    public List<HostedRepository> getAllHostedRepositories()
            throws IndyDataException
    {
        return delegate.getAllHostedRepositories();
    }

    @Override
    public List<ArtifactStore> getAllConcreteArtifactStores()
            throws IndyDataException
    {
        return delegate.getAllConcreteArtifactStores();
    }

    @Override
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( String groupName, boolean enabledOnly )
            throws IndyDataException
    {
        return delegate.getOrderedConcreteStoresInGroup( groupName, enabledOnly );
    }

    @Override
    public List<ArtifactStore> getOrderedStoresInGroup( String groupName, boolean enabledOnly )
            throws IndyDataException
    {
        return delegate.getOrderedStoresInGroup( groupName, enabledOnly );
    }

    @Override
    public Set<Group> getGroupsContaining( StoreKey repo )
            throws IndyDataException
    {
        return delegate.getGroupsContaining( repo );
    }

    @Override
    public void deleteArtifactStore( StoreKey key, ChangeSummary summary )
            throws IndyDataException
    {
        deleteArtifactStore( key, summary, new EventMetadata() );
    }

    @Override
    public void deleteArtifactStore( StoreKey key, ChangeSummary summary, EventMetadata eventMetadata )
            throws IndyDataException
    {
        delegate.deleteArtifactStore( key, summary, new EventMetadata() );
    }

    @Override
    public void clear( ChangeSummary summary )
            throws IndyDataException
    {
        delegate.clear( summary );
    }

    @Override
    public void install()
            throws IndyDataException
    {
        delegate.install();
    }

    @Override
    public void reload()
            throws IndyDataException
    {
        delegate.reload();
    }

    @Override
    public boolean hasRemoteRepository( String name )
    {
        return delegate.hasRemoteRepository( name );
    }

    @Override
    public boolean hasGroup( String name )
    {
        return delegate.hasGroup( name );
    }

    @Override
    public boolean hasHostedRepository( String name )
    {
        return delegate.hasHostedRepository( name );
    }

    @Override
    public boolean hasArtifactStore( StoreKey key )
    {
        return delegate.hasArtifactStore( key );
    }

    @Override
    public RemoteRepository findRemoteRepository( String url )
    {
        return delegate.findRemoteRepository( url );
    }

    @Override
    public boolean isStarted()
    {
        return delegate.isStarted();
    }

    @Override
    public Set<Group> getGroupsAffectedBy( StoreKey... keys )
    {
        return delegate.getGroupsAffectedBy( keys );
    }

    @Override
    public Set<Group> getGroupsAffectedBy( Collection<StoreKey> keys )
    {
        return delegate.getGroupsAffectedBy( keys );
    }

    @Override
    public boolean checkHostedReadonly( ArtifactStore store )
    {
        return delegate.checkHostedReadonly( store );
    }

    @Override
    public boolean checkHostedReadonly( StoreKey storeKey )
    {
        return delegate.checkHostedReadonly( storeKey );
    }
}
