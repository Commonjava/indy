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
import org.commonjava.util.logging.Logger;
import org.commonjava.util.logging.helper.JoinString;

@javax.enterprise.context.ApplicationScoped
public class MemoryStoreDataManager
    implements StoreDataManager
{

    private final Map<StoreKey, ArtifactStore> stores = new HashMap<StoreKey, ArtifactStore>();

    private final Logger logger = new Logger( getClass() );

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
        logger.info( "Getting repository for: %s\n\nAll stores:\n\t%s", key, new JoinString( "\n\t", stores.values() ) );

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
                    if ( groupContains( embedded, key ) )
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

    private boolean store( final ArtifactStore store, final boolean skipIfExists )
    {
        if ( skipIfExists && stores.containsKey( store.getKey() ) )
        {
            return false;
        }

        stores.put( store.getKey(), store );
        return true;
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
    public void install()
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

    private void fireDeleteEvent( final StoreType type, final String... names )
    {
        final ProxyManagerDeleteEvent event = new ProxyManagerDeleteEvent( type, names );

        if ( delEvent != null )
        {
            logger.info( "Firing delete event: %s", event );
            delEvent.fire( event );
        }
        else
        {
            logger.info( "Cannot fire delete event: %s!", event );
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

}
