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
package org.commonjava.indy.data;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Provides a convenient delegating implementation of {@link ArtifactStoreQuery}, which can be extended to wrap
 * particular methods with extra logic.
 *
 * Created by jdcasey on 5/11/17.
 */
public class DelegatingArtifactStoreQuery<T extends ArtifactStore>
        implements ArtifactStoreQuery<T>
{

    private ArtifactStoreQuery<T> delegate;

    protected DelegatingArtifactStoreQuery( ArtifactStoreQuery delegate )
    {
        this.delegate = delegate;
    }

    protected final ArtifactStoreQuery<T> delegate()
    {
        return delegate;
    }

    @Override
    public ArtifactStoreQuery<T> rewrap( final StoreDataManager manager )
    {
        delegate.rewrap( manager );
        return this;
    }

    @Override
    public ArtifactStoreQuery<T> packageType( final String packageType )
            throws IndyDataException
    {
        delegate.packageType( packageType );
        return this;
    }

    @Override
    public <C extends ArtifactStore> ArtifactStoreQuery<C> storeType( final Class<C> storeCls )
    {
        delegate.storeType( storeCls );
        return (ArtifactStoreQuery<C>) this;
    }

    @Override
    public ArtifactStoreQuery<T> storeTypes( final StoreType... types )
    {
        delegate.storeTypes( types );
        return this;
    }

    @Override
    public ArtifactStoreQuery<T> concreteStores()
    {
        delegate.concreteStores();
        return this;
    }

    @Override
    public ArtifactStoreQuery<T> enabledState( final Boolean enabled )
    {
        delegate.enabledState( enabled );
        return this;
    }

    @Override
    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    @Override
    public List<T> getAll()
            throws IndyDataException
    {
        return delegate.getAll();
    }

    @Override
    public Stream<T> stream()
            throws IndyDataException
    {
        return delegate.stream();
    }

    @Override
    public Stream<T> stream( final Predicate<ArtifactStore> filter )
            throws IndyDataException
    {
        return delegate.stream( filter );
    }

    @Override
    public List<T> getAll( final Predicate<ArtifactStore> filter )
            throws IndyDataException
    {
        return delegate.getAll( filter );
    }

    @Override
    public List<T> getAllByDefaultPackageTypes()
            throws IndyDataException
    {
        return delegate.getAllByDefaultPackageTypes();
    }

    @Override
    public T getByName( final String name )
            throws IndyDataException
    {
        return delegate.getByName( name );
    }

    @Override
    public boolean containsByName( final String name )
            throws IndyDataException
    {
        return delegate.containsByName( name );
    }

    @Override
    public Set<Group> getGroupsContaining( final StoreKey storeKey )
            throws IndyDataException
    {
        return delegate.getGroupsContaining( storeKey );
    }

    @Override
    public List<RemoteRepository> getRemoteRepositoryByUrl( final String url )
            throws IndyDataException
    {
        return delegate.getRemoteRepositoryByUrl( url );
    }

    @Override
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
            throws IndyDataException
    {
        return delegate.getOrderedConcreteStoresInGroup( groupName );
    }

    @Override
    public List<ArtifactStore> getOrderedStoresInGroup( final String groupName )
            throws IndyDataException
    {
        return delegate.getOrderedStoresInGroup( groupName );
    }

    @Override
    public Set<Group> getGroupsAffectedBy( final StoreKey... keys )
            throws IndyDataException
    {
        return delegate.getGroupsAffectedBy( keys );
    }

    @Override
    public Set<Group> getGroupsAffectedBy( final Collection<StoreKey> keys )
            throws IndyDataException
    {
        return delegate.getGroupsAffectedBy( keys );
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
    public List<Group> getAllGroups()
            throws IndyDataException
    {
        return delegate.getAllGroups();
    }

    @Override
    public RemoteRepository getRemoteRepository( final String name )
            throws IndyDataException
    {
        return delegate.getRemoteRepository( name );
    }

    @Override
    public HostedRepository getHostedRepository( final String name )
            throws IndyDataException
    {
        return delegate.getHostedRepository( name );
    }

    @Override
    public Group getGroup( final String name )
            throws IndyDataException
    {
        return delegate.getGroup( name );
    }

    @Override
    public ArtifactStoreQuery<T> noPackageType()
    {
        delegate.noPackageType();
        return this;
    }
}
