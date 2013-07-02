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

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.commonjava.aprox.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.change.event.ProxyManagerDeleteEvent;
import org.commonjava.aprox.change.event.ProxyManagerUpdateType;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;

@javax.enterprise.context.ApplicationScoped
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
    public DeployPoint getDeployPoint( final String name )
        throws ProxyDataException
    {
        return (DeployPoint) stores.get( new StoreKey( StoreType.deploy_point, name ) );
    }

    @Override
    public ArtifactStore getArtifactStore( final StoreKey key )
        throws ProxyDataException
    {
        return stores.get( key );
    }

    @Override
    public Repository getRepository( final String name )
        throws ProxyDataException
    {
        final StoreKey key = new StoreKey( StoreType.repository, name );

        return (Repository) stores.get( key );
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
    public List<Repository> getAllRepositories()
        throws ProxyDataException
    {
        return getAll( StoreType.repository, Repository.class );
    }

    @Override
    public List<DeployPoint> getAllDeployPoints()
        throws ProxyDataException
    {
        return getAll( StoreType.deploy_point, DeployPoint.class );
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
    public void storeDeployPoints( final Collection<DeployPoint> deploys )
        throws ProxyDataException
    {
        for ( final DeployPoint deploy : deploys )
        {
            store( deploy, false );
        }
        fireStoreEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, deploys );
    }

    @Override
    public boolean storeDeployPoint( final DeployPoint deploy )
        throws ProxyDataException
    {
        return store( deploy, false );
    }

    @Override
    public boolean storeDeployPoint( final DeployPoint deploy, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = store( deploy, skipIfExists );
        fireStoreEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, deploy );

        return result;
    }

    @Override
    public void storeRepositories( final Collection<Repository> repos )
        throws ProxyDataException
    {
        for ( final Repository repository : repos )
        {
            store( repository, false );
        }
        fireStoreEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, repos );
    }

    @Override
    public boolean storeRepository( final Repository repository )
        throws ProxyDataException
    {
        return store( repository, false );
    }

    @Override
    public boolean storeRepository( final Repository repository, final boolean skipIfExists )
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
    public void deleteDeployPoint( final DeployPoint deploy )
        throws ProxyDataException
    {
        stores.remove( deploy.getKey() );
        fireDeleteEvent( StoreType.deploy_point, deploy.getName() );
    }

    @Override
    public void deleteDeployPoint( final String name )
        throws ProxyDataException
    {
        stores.remove( new StoreKey( StoreType.deploy_point, name ) );
        fireDeleteEvent( StoreType.deploy_point, name );
    }

    @Override
    public void deleteRepository( final Repository repo )
        throws ProxyDataException
    {
        stores.remove( repo.getKey() );
        fireDeleteEvent( StoreType.repository, repo.getName() );
    }

    @Override
    public void deleteRepository( final String name )
        throws ProxyDataException
    {
        stores.remove( new StoreKey( StoreType.repository, name ) );
        fireDeleteEvent( StoreType.repository, name );
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

    private void fireStoreEvent( final ProxyManagerUpdateType type, final Repository... repos )
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
        return getAll( StoreType.deploy_point, StoreType.repository );
    }

    @Override
    public List<? extends ArtifactStore> getAllArtifactStores( final StoreType type )
        throws ProxyDataException
    {
        return getAll( type, type.getStoreClass() );
    }

    @Override
    public boolean hasRepository( final String name )
    {
        return hasArtifactStore( new StoreKey( StoreType.repository, name ) );
    }

    @Override
    public boolean hasGroup( final String name )
    {
        return hasArtifactStore( new StoreKey( StoreType.group, name ) );
    }

    @Override
    public boolean hasDeployPoint( final String name )
    {
        return hasArtifactStore( new StoreKey( StoreType.deploy_point, name ) );
    }

    @Override
    public boolean hasArtifactStore( final StoreKey key )
    {
        return stores.containsKey( key );
    }

}
