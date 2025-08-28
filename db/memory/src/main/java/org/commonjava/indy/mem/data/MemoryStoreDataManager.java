/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.mem.data;

import org.commonjava.cdi.util.weft.NamedThreadFactory;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.NoOpStoreEventDispatcher;
import org.commonjava.indy.data.StoreEventDispatcher;
import org.commonjava.indy.db.common.AbstractStoreDataManager;
import org.commonjava.indy.db.common.inject.Standalone;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
@Standalone
public class MemoryStoreDataManager
        extends AbstractStoreDataManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Map<StoreKey, ArtifactStore> stores = new ConcurrentHashMap<>();

    @Inject
    private StoreEventDispatcher dispatcher;

    protected MemoryStoreDataManager()
    {
    }

    public MemoryStoreDataManager( final boolean unitTestUsage )
    {
        this.dispatcher = new NoOpStoreEventDispatcher();
        if ( unitTestUsage )
        {
            super.affectedByAsyncRunner = Executors.newFixedThreadPool( 4, new NamedThreadFactory(
                            AFFECTED_BY_ASYNC_RUNNER_NAME, new ThreadGroup( AFFECTED_BY_ASYNC_RUNNER_NAME ), true,
                            4 ) );
        }
    }

    public MemoryStoreDataManager( final StoreEventDispatcher dispatcher )
    {
        this.dispatcher = dispatcher;
    }

    @Override
    protected StoreEventDispatcher getStoreEventDispatcher()
    {
        return dispatcher;
    }

    @Override
    protected ArtifactStore getArtifactStoreInternal( StoreKey key )
    {
        return stores.get( key );
    }

    @Override
    protected void removeAffectedBy( StoreKey key, StoreKey affected )
    {

    }

    @Override
    protected void addAffectedBy( StoreKey key, StoreKey affected )
    {

    }

    @Override
    protected void removeAffectedStore( StoreKey key )
    {

    }

    @Override
    protected ArtifactStore removeArtifactStoreInternal( StoreKey key )
    {
        return stores.remove( key );
    }

    @Override
    public void install()
            throws IndyDataException
    {
    }

    @Override
    public void clear( final ChangeSummary summary )
            throws IndyDataException
    {
        stores.clear();
    }

    @Override
    public Set<ArtifactStore> getAllArtifactStores()
            throws IndyDataException
    {
        return new HashSet<>( stores.values() );
    }

    @Override
    public Map<StoreKey, ArtifactStore> getArtifactStoresByKey()
    {
        return new HashMap<>( stores );
    }

    @Override
    public boolean hasArtifactStore( final StoreKey key )
    {
        return stores.containsKey( key );
    }

    @Override
    public void reload()
            throws IndyDataException
    {
    }

    @Override
    public boolean isStarted()
    {
        return true;
    }

    @Override
    public boolean isEmpty()
    {
        return stores.isEmpty();
    }

    @Override
    public Stream<StoreKey> streamArtifactStoreKeys()
    {
        return stores.keySet().stream();
    }

    @Override
    public Set<ArtifactStore> getArtifactStoresByPkgAndType( String packageType, StoreType storeType )
    {
        return stores.values()
                     .stream()
                     .filter( item -> packageType.equals( item.getPackageType() ) && storeType.equals(
                                     item.getType() ) )
                     .collect( Collectors.toSet() );
    }

    @Override
    protected ArtifactStore putArtifactStoreInternal( StoreKey storeKey, ArtifactStore store )
    {
        return stores.put( storeKey, store );
    }

    @Override
    public void addConstituentToGroup( StoreKey key, StoreKey member )
            throws IndyWorkflowException
    {
    }
}
