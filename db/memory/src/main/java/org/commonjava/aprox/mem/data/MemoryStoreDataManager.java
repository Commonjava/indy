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
package org.commonjava.aprox.mem.data;

import static org.commonjava.aprox.model.StoreType.group;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import org.commonjava.aprox.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.change.event.ProxyManagerDeleteEvent;
import org.commonjava.aprox.change.event.ProxyManagerUpdateType;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;

@ApplicationScoped
@Alternative
public class MemoryStoreDataManager
    implements StoreDataManager
{

    private final Map<StoreKey, ArtifactStore> stores = new HashMap<StoreKey, ArtifactStore>();

    //    private final Logger logger = new Logger( getClass() );

    @Inject
    private Event<ArtifactStoreUpdateEvent> storeEvent;

    @Inject
    private Event<ProxyManagerDeleteEvent> delEvent;

    public MemoryStoreDataManager()
    {
    }

    @Override
    public HostedRepository getHostedRepository( final String name )
        throws ProxyDataException
    {
        return (HostedRepository) stores.get( new StoreKey( StoreType.hosted, name ) );
    }

    @Override
    public ArtifactStore getArtifactStore( final StoreKey key )
        throws ProxyDataException
    {
        return stores.get( key );
    }

    @Override
    public RemoteRepository getRemoteRepository( final String name )
        throws ProxyDataException
    {
        final StoreKey key = new StoreKey( StoreType.remote, name );

        return (RemoteRepository) stores.get( key );
    }

    @Override
    public Group getGroup( final String name )
        throws ProxyDataException
    {
        return (Group) stores.get( new StoreKey( StoreType.group, name ) );
    }

    @Override
    public List<Group> getAllGroups()
        throws ProxyDataException
    {
        return getAll( StoreType.group, Group.class );
    }

    @Override
    public List<RemoteRepository> getAllRemoteRepositories()
        throws ProxyDataException
    {
        return getAll( StoreType.remote, RemoteRepository.class );
    }

    @Override
    public List<HostedRepository> getAllHostedRepositories()
        throws ProxyDataException
    {
        return getAll( StoreType.hosted, HostedRepository.class );
    }

    @Override
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
        throws ProxyDataException
    {
        return getGroupOrdering( groupName, false );
    }

    @Override
    public List<ArtifactStore> getOrderedStoresInGroup( final String groupName )
        throws ProxyDataException
    {
        return getGroupOrdering( groupName, true );
    }

    private List<ArtifactStore> getGroupOrdering( final String groupName, final boolean includeGroups )
        throws ProxyDataException
    {
        final Group master = (Group) stores.get( new StoreKey( StoreType.group, groupName ) );
        if ( master == null )
        {
            return Collections.emptyList();
        }

        final List<ArtifactStore> result = new ArrayList<ArtifactStore>();
        recurseGroup( master, result, includeGroups );

        return result;
    }

    private void recurseGroup( final Group master, final List<ArtifactStore> result, final boolean includeGroups )
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
            if ( type == StoreType.group )
            {
                recurseGroup( (Group) stores.get( key ), result, includeGroups );
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
        throws ProxyDataException
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

    private boolean groupContains( final Group g, final StoreKey key )
    {
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
    public void storeHostedRepositories( final Collection<HostedRepository> repos )
        throws ProxyDataException
    {
        for ( final HostedRepository repo : repos )
        {
            store( repo, false );
        }
        fireStoreEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, repos );
    }

    @Override
    public boolean storeHostedRepository( final HostedRepository repo )
        throws ProxyDataException
    {
        return store( repo, false );
    }

    @Override
    public boolean storeHostedRepository( final HostedRepository repo, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = store( repo, skipIfExists );
        fireStoreEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, repo );

        return result;
    }

    @Override
    public void storeRemoteRepositories( final Collection<RemoteRepository> repos )
        throws ProxyDataException
    {
        for ( final RemoteRepository repository : repos )
        {
            store( repository, false );
        }
        fireStoreEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, repos );
    }

    @Override
    public boolean storeRemoteRepository( final RemoteRepository repository )
        throws ProxyDataException
    {
        return store( repository, false );
    }

    @Override
    public boolean storeRemoteRepository( final RemoteRepository repository, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = store( repository, skipIfExists );
        fireStoreEvent( skipIfExists ? ProxyManagerUpdateType.ADD : ProxyManagerUpdateType.ADD_OR_UPDATE, repository );
        return result;
    }

    @Override
    public void storeGroups( final Collection<Group> groups )
        throws ProxyDataException
    {
        for ( final Group group : groups )
        {
            store( group, false );
        }
        fireStoreEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, groups );
    }

    @Override
    public boolean storeGroup( final Group group )
        throws ProxyDataException
    {
        return store( group, false );
    }

    @Override
    public boolean storeGroup( final Group group, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = store( group, skipIfExists );
        fireStoreEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, group );

        return result;
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore store )
        throws ProxyDataException
    {
        return store( store, false );
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore store, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = store( store, skipIfExists );
        fireStoreEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, store );

        return result;
    }

    private boolean store( final ArtifactStore store, final boolean skipIfExists )
    {
        if ( !skipIfExists || !stores.containsKey( store.getKey() ) )
        {
            stores.put( store.getKey(), store );
            return true;
        }

        return false;
    }

    @Override
    public void deleteHostedRepository( final HostedRepository repo )
        throws ProxyDataException
    {
        stores.remove( repo.getKey() );
        fireDeleteEvent( StoreType.hosted, repo.getName() );
    }

    @Override
    public void deleteHostedRepository( final String name )
        throws ProxyDataException
    {
        stores.remove( new StoreKey( StoreType.hosted, name ) );
        fireDeleteEvent( StoreType.hosted, name );
    }

    @Override
    public void deleteRemoteRepository( final RemoteRepository repo )
        throws ProxyDataException
    {
        stores.remove( repo.getKey() );
        fireDeleteEvent( StoreType.remote, repo.getName() );
    }

    @Override
    public void deleteRemoteRepository( final String name )
        throws ProxyDataException
    {
        stores.remove( new StoreKey( StoreType.remote, name ) );
        fireDeleteEvent( StoreType.remote, name );
    }

    @Override
    public void deleteGroup( final Group group )
        throws ProxyDataException
    {
        stores.remove( group.getKey() );
        fireDeleteEvent( StoreType.group, group.getName() );
    }

    @Override
    public void deleteGroup( final String name )
        throws ProxyDataException
    {
        stores.remove( new StoreKey( StoreType.group, name ) );
        fireDeleteEvent( StoreType.group, name );
    }

    @Override
    public void deleteArtifactStore( final StoreKey key )
        throws ProxyDataException
    {
        stores.remove( key );
        fireDeleteEvent( key.getType(), key.getName() );
    }

    @Override
    public void install()
        throws ProxyDataException
    {
    }

    @Override
    public void clear()
        throws ProxyDataException
    {
        stores.clear();
    }

    private <T extends ArtifactStore> List<T> getAll( final StoreType storeType, final Class<T> type )
    {
        final List<T> result = new ArrayList<T>();
        for ( final Map.Entry<StoreKey, ArtifactStore> store : stores.entrySet() )
        {
            if ( store.getKey()
                      .getType() == storeType )
            {
                result.add( type.cast( store.getValue() ) );
            }
        }

        return result;
    }

    private List<ArtifactStore> getAll( final StoreType... storeTypes )
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

    private void fireDeleteEvent( final StoreType type, final String... names )
    {
        final ProxyManagerDeleteEvent event = new ProxyManagerDeleteEvent( type, names );

        if ( delEvent != null )
        {
            delEvent.fire( event );
        }
    }

    private void fireStoreEvent( final ProxyManagerUpdateType type, final RemoteRepository... repos )
    {
        if ( storeEvent != null )
        {
            storeEvent.fire( new ArtifactStoreUpdateEvent( type, repos ) );
        }
    }

    @SuppressWarnings( "unchecked" )
    private void fireStoreEvent( final ProxyManagerUpdateType type, final Collection<? extends ArtifactStore> stores )
    {
        if ( storeEvent != null )
        {
            storeEvent.fire( new ArtifactStoreUpdateEvent( type, (Collection<ArtifactStore>) stores ) );
        }
    }

    private void fireStoreEvent( final ProxyManagerUpdateType type, final ArtifactStore... stores )
    {
        if ( storeEvent != null )
        {
            storeEvent.fire( new ArtifactStoreUpdateEvent( type, stores ) );
        }
    }

    @Override
    public List<ArtifactStore> getAllArtifactStores()
        throws ProxyDataException
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
        throws ProxyDataException
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
        throws ProxyDataException
    {
    }

}
