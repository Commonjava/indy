/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
 * This query interface is intended to be reusable across any {@link StoreDataManager} implementation. It contains logic
 * for working with the {@link ArtifactStore}s contained in the StoreDataManager, but this logic is not tied directly to
 * any data manager implementation. Separating these methods out and providing a single {@link StoreDataManager#query()}
 * method to provide access to an instance of this class drastically simplifies the task of implementing a new type of
 * data manager.
 * <p>
 * This class is intended to function as a fluent api. It does keep state on packageType, storeType(s), and
 * enabled / disabled selection. However, methods that pertain to specific types of ArtifactStore (indicated by their
 * names) DO NOT change the internal state of the query instance on which they are called.
 * <p>
 * Created by jdcasey on 5/10/17.
 */
public interface ArtifactStoreQuery<T extends ArtifactStore>
{

    <C extends ArtifactStore> ArtifactStoreQuery<C> storeType( Class<C> storeCls );

    ArtifactStoreQuery<T> storeTypes( StoreType... types );

    ArtifactStoreQuery<T> concreteStores();

    ArtifactStoreQuery<T> enabledState( Boolean enabled );

//    boolean isEmpty();

    List<T> getAll()
            throws IndyDataException;

    @Deprecated
    List<T> getAll( Predicate<ArtifactStore> filter )
            throws IndyDataException;

    List<T> getAllByDefaultPackageTypes()
            throws IndyDataException;

    T getByName( String name )
            throws IndyDataException;

    Set<Group> getGroupsContaining( StoreKey storeKey )
            throws IndyDataException;

    Set<Group> getGroupsContaining( StoreKey storeKey, Boolean enabled )
                    throws IndyDataException;

    List<RemoteRepository> getRemoteRepositoryByUrl( String packageType, String url )
            throws IndyDataException;

    List<RemoteRepository> getRemoteRepositoryByUrl( String packageType, String url, Boolean enabled )
                    throws IndyDataException;

    List<ArtifactStore> getOrderedConcreteStoresInGroup( String packageType, String groupName )
            throws IndyDataException;

    List<ArtifactStore> getOrderedConcreteStoresInGroup( String packageType, String groupName, Boolean enabled )
                    throws IndyDataException;

    List<ArtifactStore> getOrderedStoresInGroup( String packageType, String groupName )
            throws IndyDataException;

    List<ArtifactStore> getOrderedStoresInGroup( String packageType, String groupName, Boolean enabled )
                    throws IndyDataException;

    Set<Group> getGroupsAffectedBy( StoreKey... keys )
            throws IndyDataException;

    Set<Group> getGroupsAffectedBy( Collection<StoreKey> keys )
            throws IndyDataException;

    RemoteRepository getRemoteRepository( String packageType, String name )
            throws IndyDataException;

    HostedRepository getHostedRepository( String packageType, String name )
            throws IndyDataException;

    Group getGroup( String packageType, String name )
            throws IndyDataException;

    ArtifactStoreQuery<T> noPackageType();

    List<RemoteRepository> getAllRemoteRepositories( String packageType )
                    throws IndyDataException;

    List<RemoteRepository> getAllRemoteRepositories( String packageType, Boolean enabled )
                    throws IndyDataException;

    List<HostedRepository> getAllHostedRepositories( String packageType )
                    throws IndyDataException;

    List<HostedRepository> getAllHostedRepositories( String packageType, Boolean enabled )
                    throws IndyDataException;

    List<Group> getAllGroups( String packageType )
                    throws IndyDataException;

    List<Group> getAllGroups( String packageType, Boolean enabled )
                    throws IndyDataException;
}
