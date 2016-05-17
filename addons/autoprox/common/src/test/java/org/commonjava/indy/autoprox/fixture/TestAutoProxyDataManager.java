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
package org.commonjava.indy.autoprox.fixture;

import java.util.List;
import java.util.Set;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.autoprox.data.AutoProxCatalogManager;
import org.commonjava.indy.autoprox.data.AutoProxDataManagerDecorator;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.event.EventMetadata;

public class TestAutoProxyDataManager
    extends AutoProxDataManagerDecorator
    implements StoreDataManager
{

    private final StoreDataManager delegate;

    public TestAutoProxyDataManager( final AutoProxCatalogManager catalog, final TransferManager transferManager )
    {
        super( new MemoryStoreDataManager( true ), catalog, transferManager );
        delegate = getDelegate();
    }

    @Override
    public List<ArtifactStore> getAllArtifactStores()
        throws IndyDataException
    {
        return delegate.getAllArtifactStores();
    }

    @Override
    public List<? extends ArtifactStore> getAllArtifactStores( final StoreType type )
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
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName, final boolean enabledOnly )
        throws IndyDataException
    {
        return delegate.getOrderedConcreteStoresInGroup( groupName, enabledOnly );
    }

    @Override
    public List<ArtifactStore> getOrderedStoresInGroup( final String groupName, final boolean enabledOnly )
        throws IndyDataException
    {
        return delegate.getOrderedStoresInGroup( groupName, enabledOnly );
    }

    @Override
    public Set<Group> getGroupsContaining( final StoreKey repo )
        throws IndyDataException
    {
        return delegate.getGroupsContaining( repo );
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
    public boolean storeArtifactStore( final ArtifactStore key, final ChangeSummary summary )
        throws IndyDataException
    {
        return storeArtifactStore( key, summary, new EventMetadata() );
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore key , final ChangeSummary summary , final EventMetadata eventMetadata  )
        throws IndyDataException
    {
        return delegate.storeArtifactStore( key, summary, new EventMetadata() );
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore key, final ChangeSummary summary, final boolean skipIfExists )
        throws IndyDataException
    {
        return storeArtifactStore( key, summary, skipIfExists, new EventMetadata() );
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore key , final ChangeSummary summary , final boolean skipIfExists , final EventMetadata eventMetadata  )
        throws IndyDataException
    {
        return delegate.storeArtifactStore( key, summary, skipIfExists, new EventMetadata() );
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore key, final ChangeSummary summary,
                                       final boolean skipIfExists, final boolean fireEvents )
        throws IndyDataException
    {
        return storeArtifactStore( key, summary, skipIfExists, fireEvents, new EventMetadata() );
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore key , final ChangeSummary summary ,
                                       final boolean skipIfExists , final boolean fireEvents , final EventMetadata eventMetadata  )
        throws IndyDataException
    {
        return delegate.storeArtifactStore( key, summary, skipIfExists, fireEvents, new EventMetadata() );
    }

    @Override
    public void deleteArtifactStore( final StoreKey key, final ChangeSummary summary )
        throws IndyDataException
    {
        deleteArtifactStore( key, summary, new EventMetadata() );
    }

    @Override
    public void deleteArtifactStore( final StoreKey key , final ChangeSummary summary , final EventMetadata eventMetadata  )
        throws IndyDataException
    {
        delegate.deleteArtifactStore( key, summary, new EventMetadata() );
    }

    @Override
    public void clear( final ChangeSummary summary )
        throws IndyDataException
    {
        delegate.clear( summary );
    }

    @Override
    public boolean hasRemoteRepository( final String name )
    {
        return delegate.hasRemoteRepository( name );
    }

    @Override
    public boolean hasGroup( final String name )
    {
        return delegate.hasGroup( name );
    }

    @Override
    public boolean hasHostedRepository( final String name )
    {
        return delegate.hasHostedRepository( name );
    }

    @Override
    public boolean hasArtifactStore( final StoreKey key )
    {
        return delegate.hasArtifactStore( key );
    }

    @Override
    public RemoteRepository findRemoteRepository( final String url )
    {
        return delegate.findRemoteRepository( url );
    }

    @Override
    public boolean isStarted()
    {
        return delegate.isStarted();
    }

}
