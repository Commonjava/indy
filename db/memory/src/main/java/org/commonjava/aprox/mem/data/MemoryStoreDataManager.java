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
package org.commonjava.aprox.mem.data;

import static org.commonjava.aprox.model.core.StoreType.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.change.event.ArtifactStoreUpdateType;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.NoOpStoreEventDispatcher;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.data.StoreEventDispatcher;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Alternative
public class MemoryStoreDataManager
    implements StoreDataManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Map<StoreKey, ArtifactStore> stores = new HashMap<StoreKey, ArtifactStore>();

    private final Map<String, RemoteRepository> byRemoteUrl = new HashMap<String, RemoteRepository>();

    //    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreEventDispatcher dispatcher;

    protected MemoryStoreDataManager()
    {
    }

    public MemoryStoreDataManager( final boolean unitTestUsage )
    {
        this.dispatcher = new NoOpStoreEventDispatcher();
    }

    public MemoryStoreDataManager( final StoreEventDispatcher dispatcher )
    {
        this.dispatcher = dispatcher;
    }

    @Override
    public HostedRepository getHostedRepository( final String name )
        throws AproxDataException
    {
        return (HostedRepository) stores.get( new StoreKey( StoreType.hosted, name ) );
    }

    @Override
    public ArtifactStore getArtifactStore( final StoreKey key )
        throws AproxDataException
    {
        return stores.get( key );
    }

    @Override
    public RemoteRepository getRemoteRepository( final String name )
        throws AproxDataException
    {
        final StoreKey key = new StoreKey( StoreType.remote, name );

        return (RemoteRepository) stores.get( key );
    }

    @Override
    public Group getGroup( final String name )
        throws AproxDataException
    {
        return (Group) stores.get( new StoreKey( StoreType.group, name ) );
    }

    @Override
    public List<Group> getAllGroups()
        throws AproxDataException
    {
        return getAll( StoreType.group, Group.class );
    }

    @Override
    public List<RemoteRepository> getAllRemoteRepositories()
        throws AproxDataException
    {
        return getAll( StoreType.remote, RemoteRepository.class );
    }

    @Override
    public List<HostedRepository> getAllHostedRepositories()
        throws AproxDataException
    {
        return getAll( StoreType.hosted, HostedRepository.class );
    }

    @Override
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
        throws AproxDataException
    {
        return getGroupOrdering( groupName, false, true );
    }

    @Override
    public List<ArtifactStore> getOrderedStoresInGroup( final String groupName )
        throws AproxDataException
    {
        return getGroupOrdering( groupName, true, false );
    }

    private List<ArtifactStore> getGroupOrdering( final String groupName, final boolean includeGroups,
                                                  final boolean recurseGroups )
        throws AproxDataException
    {
        final Group master = (Group) stores.get( new StoreKey( StoreType.group, groupName ) );
        if ( master == null )
        {
            return Collections.emptyList();
        }

        final List<ArtifactStore> result = new ArrayList<ArtifactStore>();
        recurseGroup( master, result, includeGroups, recurseGroups );

        return result;
    }

    private synchronized void recurseGroup( final Group master, final List<ArtifactStore> result,
                                            final boolean includeGroups, final boolean recurseGroups )
    {
        if ( master == null )
        {
            return;
        }

        if ( includeGroups )
        {
            result.add( master );
        }

        for ( final StoreKey key : master.getConstituents() )
        {
            final StoreType type = key.getType();
            if ( recurseGroups && type == StoreType.group )
            {
                // if we're here, we're definitely recursing groups...
                recurseGroup( (Group) stores.get( key ), result, includeGroups, true );
            }
            else
            {
                final ArtifactStore store = stores.get( key );
                if ( store != null )
                {
                    result.add( store );
                }
            }
        }
    }

    @Override
    public Set<Group> getGroupsContaining( final StoreKey repo )
        throws AproxDataException
    {
        final Set<Group> groups = new HashSet<Group>();
        for ( final Group group : getAllGroups() )
        {
            if ( groupContains( group, repo ) )
            {
                groups.add( group );
            }
        }

        return groups;
    }

    private synchronized boolean groupContains( final Group g, final StoreKey key )
    {
        if ( g == null || g.getConstituents() == null )
        {
            return false;
        }

        if ( g.getConstituents()
              .contains( key ) )
        {
            return true;
        }
        else
        {
            for ( final StoreKey constituent : g.getConstituents() )
            {
                if ( constituent.getType() == group )
                {
                    final Group embedded = (Group) stores.get( constituent );
                    if ( embedded != null && groupContains( embedded, key ) )
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean storeHostedRepository( final HostedRepository repo, final ChangeSummary summary )
        throws AproxDataException
    {
        return store( repo, summary, false );
    }

    @Override
    public boolean storeHostedRepository( final HostedRepository repo, final ChangeSummary summary,
                                          final boolean skipIfExists )
        throws AproxDataException
    {
        final boolean exists = stores.containsKey( repo.getKey() );
        dispatcher.updating( exists ? ArtifactStoreUpdateType.UPDATE : ArtifactStoreUpdateType.ADD, repo );

        final boolean result = store( repo, summary, skipIfExists );

        return result;
    }

    @Override
    public boolean storeRemoteRepository( final RemoteRepository repository, final ChangeSummary summary )
        throws AproxDataException
    {
        final boolean result = store( repository, summary, false );
        if ( result )
        {
            byRemoteUrl.put( repository.getUrl(), repository );
        }
        return result;
    }

    @Override
    public boolean storeRemoteRepository( final RemoteRepository repository, final ChangeSummary summary,
                                          final boolean skipIfExists )
        throws AproxDataException
    {
        final boolean exists = stores.containsKey( repository.getKey() );
        dispatcher.updating( exists ? ArtifactStoreUpdateType.UPDATE : ArtifactStoreUpdateType.ADD, repository );

        final boolean result = store( repository, summary, skipIfExists );
        if ( result )
        {
            byRemoteUrl.put( repository.getUrl(), repository );
        }

        return result;
    }

    @Override
    public boolean storeGroup( final Group group, final ChangeSummary summary )
        throws AproxDataException
    {
        return store( group, summary, false );
    }

    @Override
    public boolean storeGroup( final Group group, final ChangeSummary summary, final boolean skipIfExists )
        throws AproxDataException
    {
        final boolean result = store( group, summary, skipIfExists );

        return result;
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore store, final ChangeSummary summary )
        throws AproxDataException
    {
        final boolean result = store( store, summary, false );
        if ( result && ( store instanceof RemoteRepository ) )
        {
            final RemoteRepository repository = (RemoteRepository) store;
            byRemoteUrl.put( repository.getUrl(), repository );
        }

        return result;
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore store, final ChangeSummary summary,
                                       final boolean skipIfExists )
        throws AproxDataException
    {
        final boolean result = store( store, summary, skipIfExists );
        if ( result && ( store instanceof RemoteRepository ) )
        {
            final RemoteRepository repository = (RemoteRepository) store;
            byRemoteUrl.put( repository.getUrl(), repository );
        }

        return result;
    }

    private synchronized boolean store( final ArtifactStore store, final ChangeSummary summary,
                                        final boolean skipIfExists )
        throws AproxDataException
    {
        final boolean exists = stores.containsKey( store.getKey() );
        if ( !skipIfExists || !exists )
        {
            preStore( store, summary, exists );
            final ArtifactStore old = stores.put( store.getKey(), store );
            try
            {
                postStore( store, summary, exists );
                return true;
            }
            catch ( final AproxDataException e )
            {
                logger.error( "postStore() failed for: {}. Rolling back to old value: {}", store, old );
                stores.put( old.getKey(), old );
            }
        }

        return false;
    }

    protected void preStore( final ArtifactStore store, final ChangeSummary summary, final boolean exists )
        throws AproxDataException
    {
        dispatcher.updating( exists ? ArtifactStoreUpdateType.UPDATE : ArtifactStoreUpdateType.ADD, store );
    }

    protected void postStore( final ArtifactStore store, final ChangeSummary summary, final boolean exists )
        throws AproxDataException
    {
        dispatcher.updated( exists ? ArtifactStoreUpdateType.UPDATE : ArtifactStoreUpdateType.ADD, store );
    }

    @Override
    public synchronized void deleteHostedRepository( final HostedRepository repo, final ChangeSummary summary )
        throws AproxDataException
    {
        if ( !stores.containsKey( repo.getKey() ) )
        {
            return;
        }

        dispatcher.deleting( repo );
        stores.remove( repo.getKey() );
        dispatcher.deleted( repo );
    }

    @Override
    public synchronized void deleteHostedRepository( final String name, final ChangeSummary summary )
        throws AproxDataException
    {
        final ArtifactStore store = stores.get( new StoreKey( StoreType.hosted, name ) );
        if ( store == null )
        {
            return;
        }
        dispatcher.deleting( store );
        stores.remove( new StoreKey( StoreType.hosted, name ) );
        dispatcher.deleted( store );
    }

    @Override
    public synchronized void deleteRemoteRepository( final RemoteRepository repo, final ChangeSummary summary )
        throws AproxDataException
    {
        if ( !stores.containsKey( repo.getKey() ) )
        {
            return;
        }

        dispatcher.deleting( repo );

        stores.remove( repo.getKey() );
        dispatcher.deleted( repo );
    }

    @Override
    public synchronized void deleteRemoteRepository( final String name, final ChangeSummary summary )
        throws AproxDataException
    {
        final StoreKey key = new StoreKey( StoreType.remote, name );
        final ArtifactStore store = stores.get( key );
        if ( store == null )
        {
            return;
        }
        dispatcher.deleting( store );

        stores.remove( key );
        dispatcher.deleted( store );
    }

    @Override
    public synchronized void deleteGroup( final Group group, final ChangeSummary summary )
        throws AproxDataException
    {
        if ( !stores.containsKey( group.getKey() ) )
        {
            return;
        }
        dispatcher.deleting( group );

        stores.remove( group.getKey() );
        dispatcher.deleted( group );
    }

    @Override
    public synchronized void deleteGroup( final String name, final ChangeSummary summary )
        throws AproxDataException
    {
        final StoreKey key = new StoreKey( StoreType.group, name );
        final ArtifactStore store = stores.get( key );
        if ( store == null )
        {
            return;
        }
        dispatcher.deleting( store );

        stores.remove( key );
        dispatcher.deleted( store );
    }

    @Override
    public synchronized void deleteArtifactStore( final StoreKey key, final ChangeSummary summary )
        throws AproxDataException
    {
        final ArtifactStore store = stores.get( key );
        if ( store == null )
        {
            return;
        }

        dispatcher.deleting( store );

        stores.remove( key );
        dispatcher.deleted( store );
    }

    @Override
    public void install()
        throws AproxDataException
    {
    }

    @Override
    public void clear( final ChangeSummary summary )
        throws AproxDataException
    {
        stores.clear();
    }

    private synchronized <T extends ArtifactStore> List<T> getAll( final StoreType storeType, final Class<T> type )
    {
        final List<T> result = new ArrayList<T>();
        for ( final Map.Entry<StoreKey, ArtifactStore> store : stores.entrySet() )
        {
            if ( store.getValue() == null )
            {
                continue;
            }

            if ( store.getKey()
                      .getType() == storeType )
            {
                result.add( type.cast( store.getValue() ) );
            }
        }

        return result;
    }

    private synchronized List<ArtifactStore> getAll( final StoreType... storeTypes )
    {
        final List<ArtifactStore> result = new ArrayList<ArtifactStore>();
        for ( final Map.Entry<StoreKey, ArtifactStore> store : stores.entrySet() )
        {
            for ( final StoreType type : storeTypes )
            {
                if ( store.getKey()
                          .getType() == type )
                {
                    result.add( store.getValue() );
                }
            }
        }

        return result;
    }

    @Override
    public List<ArtifactStore> getAllArtifactStores()
        throws AproxDataException
    {
        return new ArrayList<ArtifactStore>( stores.values() );
    }

    @Override
    public List<ArtifactStore> getAllConcreteArtifactStores()
    {
        return getAll( StoreType.hosted, StoreType.remote );
    }

    @Override
    public List<? extends ArtifactStore> getAllArtifactStores( final StoreType type )
        throws AproxDataException
    {
        return getAll( type, type.getStoreClass() );
    }

    @Override
    public boolean hasRemoteRepository( final String name )
    {
        return hasArtifactStore( new StoreKey( StoreType.remote, name ) );
    }

    @Override
    public boolean hasGroup( final String name )
    {
        return hasArtifactStore( new StoreKey( StoreType.group, name ) );
    }

    @Override
    public boolean hasHostedRepository( final String name )
    {
        return hasArtifactStore( new StoreKey( StoreType.hosted, name ) );
    }

    @Override
    public boolean hasArtifactStore( final StoreKey key )
    {
        return stores.containsKey( key );
    }

    @Override
    public void reload()
        throws AproxDataException
    {
    }

    @Override
    public RemoteRepository findRemoteRepository( final String url )
    {
        return byRemoteUrl.get( url );
    }

}
