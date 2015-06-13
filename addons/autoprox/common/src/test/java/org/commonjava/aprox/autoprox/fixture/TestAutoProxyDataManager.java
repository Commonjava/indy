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
package org.commonjava.aprox.autoprox.fixture;

import java.util.List;
import java.util.Set;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.autoprox.data.AutoProxCatalogManager;
import org.commonjava.aprox.autoprox.data.AutoProxDataManagerDecorator;
import org.commonjava.aprox.core.data.DefaultStoreEventDispatcher;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.subsys.http.AproxHttpProvider;
import org.commonjava.maven.galley.event.EventMetadata;

public class TestAutoProxyDataManager
    extends AutoProxDataManagerDecorator
    implements StoreDataManager
{

    private final StoreDataManager delegate;

    public TestAutoProxyDataManager( final AutoProxCatalogManager catalog, final AproxHttpProvider http )
    {
        super( new MemoryStoreDataManager( new DefaultStoreEventDispatcher() ), catalog, http );
        delegate = getDelegate();
    }

    @Override
    public List<ArtifactStore> getAllArtifactStores()
        throws AproxDataException
    {
        return delegate.getAllArtifactStores();
    }

    @Override
    public List<? extends ArtifactStore> getAllArtifactStores( final StoreType type )
        throws AproxDataException
    {
        return delegate.getAllArtifactStores( type );
    }

    @Override
    public List<Group> getAllGroups()
        throws AproxDataException
    {
        return delegate.getAllGroups();
    }

    @Override
    public List<RemoteRepository> getAllRemoteRepositories()
        throws AproxDataException
    {
        return delegate.getAllRemoteRepositories();
    }

    @Override
    public List<HostedRepository> getAllHostedRepositories()
        throws AproxDataException
    {
        return delegate.getAllHostedRepositories();
    }

    @Override
    public List<ArtifactStore> getAllConcreteArtifactStores()
        throws AproxDataException
    {
        return delegate.getAllConcreteArtifactStores();
    }

    @Override
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
        throws AproxDataException
    {
        return delegate.getOrderedConcreteStoresInGroup( groupName );
    }

    @Override
    public List<ArtifactStore> getOrderedStoresInGroup( final String groupName )
        throws AproxDataException
    {
        return delegate.getOrderedStoresInGroup( groupName );
    }

    @Override
    public Set<Group> getGroupsContaining( final StoreKey repo )
        throws AproxDataException
    {
        return delegate.getGroupsContaining( repo );
    }

    @Override
    public void install()
        throws AproxDataException
    {
        delegate.install();
    }

    @Override
    public void reload()
        throws AproxDataException
    {
        delegate.reload();
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore key, final ChangeSummary summary )
        throws AproxDataException
    {
        return storeArtifactStore( key, summary, new EventMetadata() );
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore key , final ChangeSummary summary , final EventMetadata eventMetadata  )
        throws AproxDataException
    {
        return delegate.storeArtifactStore( key, summary, new EventMetadata() );
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore key, final ChangeSummary summary, final boolean skipIfExists )
        throws AproxDataException
    {
        return storeArtifactStore( key, summary, skipIfExists, new EventMetadata() );
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore key , final ChangeSummary summary , final boolean skipIfExists , final EventMetadata eventMetadata  )
        throws AproxDataException
    {
        return delegate.storeArtifactStore( key, summary, skipIfExists, new EventMetadata() );
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore key, final ChangeSummary summary,
                                       final boolean skipIfExists, final boolean fireEvents )
        throws AproxDataException
    {
        return storeArtifactStore( key, summary, skipIfExists, fireEvents, new EventMetadata() );
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore key , final ChangeSummary summary ,
                                       final boolean skipIfExists , final boolean fireEvents , final EventMetadata eventMetadata  )
        throws AproxDataException
    {
        return delegate.storeArtifactStore( key, summary, skipIfExists, fireEvents, new EventMetadata() );
    }

    @Override
    public void deleteArtifactStore( final StoreKey key, final ChangeSummary summary )
        throws AproxDataException
    {
        deleteArtifactStore( key, summary, new EventMetadata() );
    }

    @Override
    public void deleteArtifactStore( final StoreKey key , final ChangeSummary summary , final EventMetadata eventMetadata  )
        throws AproxDataException
    {
        delegate.deleteArtifactStore( key, summary, new EventMetadata() );
    }

    @Override
    public void clear( final ChangeSummary summary )
        throws AproxDataException
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
