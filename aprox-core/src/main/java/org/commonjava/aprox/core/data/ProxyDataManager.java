/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.core.data;

import static org.commonjava.couch.util.IdUtils.namespaceId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.core.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.core.change.event.ProxyManagerDeleteEvent;
import org.commonjava.aprox.core.change.event.ProxyManagerUpdateType;
import org.commonjava.aprox.core.conf.ProxyConfiguration;
import org.commonjava.aprox.core.data.ProxyAppDescription.View;
import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.aprox.core.model.io.StoreDeserializer;
import org.commonjava.aprox.core.model.io.StoreKeySerializer;
import org.commonjava.auth.couch.data.UserDataException;
import org.commonjava.auth.couch.data.UserDataManager;
import org.commonjava.auth.couch.model.Permission;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.db.CouchDBException;
import org.commonjava.couch.db.CouchManager;
import org.commonjava.couch.io.Serializer;
import org.commonjava.couch.model.CouchApp;
import org.commonjava.couch.model.CouchDocRef;
import org.commonjava.couch.util.JoinString;

@Singleton
public class ProxyDataManager
{
    // private final Logger logger = new Logger( getClass() );

    @Inject
    private UserDataManager userMgr;

    @Inject
    @AproxData
    private CouchManager couch;

    @Inject
    private ProxyConfiguration config;

    @Inject
    @AproxData
    private CouchDBConfiguration couchConfig;

    @Inject
    private Event<ArtifactStoreUpdateEvent> storeEvent;

    @Inject
    private Event<ProxyManagerDeleteEvent> delEvent;

    @Inject
    private Serializer serializer;

    public ProxyDataManager()
    {}

    public ProxyDataManager( final ProxyConfiguration config, final UserDataManager userMgr,
                             final CouchDBConfiguration couchConfig, final CouchManager couch,
                             final Serializer serializer )
    {
        this.config = config;
        this.userMgr = userMgr;
        this.couchConfig = couchConfig;
        this.couch = couch;
        this.serializer = serializer;

        registerSerializationAdapters();
    }

    @PostConstruct
    protected void registerSerializationAdapters()
    {
        serializer.registerSerializationAdapters( new StoreKeySerializer(), new StoreDeserializer() );
    }

    public DeployPoint getDeployPoint( final String name )
        throws ProxyDataException
    {
        try
        {
            return couch.getDocument( new CouchDocRef( namespaceId( StoreType.deploy_point.name(),
                                                                    name ) ), DeployPoint.class );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve deploy-store: %s. Reason: %s", e,
                                          name, e.getMessage() );
        }
    }

    public Repository getRepository( final String name )
        throws ProxyDataException
    {
        try
        {
            return couch.getDocument( new CouchDocRef( namespaceId( StoreType.repository.name(),
                                                                    name ) ), Repository.class );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve repository: %s. Reason: %s", e, name,
                                          e.getMessage() );
        }
    }

    public Group getGroup( final String name )
        throws ProxyDataException
    {
        try
        {
            return couch.getDocument( new CouchDocRef( namespaceId( StoreType.group.name(), name ) ),
                                      Group.class );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve group: %s. Reason: %s", e, name,
                                          e.getMessage() );
        }
    }

    public List<Group> getAllGroups()
        throws ProxyDataException
    {
        try
        {
            return couch.getViewListing( new ProxyViewRequest( config, View.ALL_GROUPS ),
                                         Group.class );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve group listing. Reason: %s", e,
                                          e.getMessage() );
        }
    }

    public List<Repository> getAllRepositories()
        throws ProxyDataException
    {
        try
        {
            return couch.getViewListing( new ProxyViewRequest( config, View.ALL_REPOSITORIES ),
                                         Repository.class );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve repository listing. Reason: %s", e,
                                          e.getMessage() );
        }
    }

    public List<DeployPoint> getAllDeployPoints()
        throws ProxyDataException
    {
        try
        {
            return couch.getViewListing( new ProxyViewRequest( config, View.ALL_REPOSITORIES ),
                                         DeployPoint.class );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve deploy-store listing. Reason: %s", e,
                                          e.getMessage() );
        }
    }

    public List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
        throws ProxyDataException
    {
        List<ArtifactStore> result = new ArrayList<ArtifactStore>();
        findOrderedConcreteStores( groupName, result );

        return result;
    }

    private void findOrderedConcreteStores( final String groupName,
                                            final List<ArtifactStore> accumStores )
        throws ProxyDataException
    {
        LinkedList<String> todo = new LinkedList<String>();
        Set<String> done = new HashSet<String>();

        todo.addLast( groupName );
        while ( !todo.isEmpty() )
        {
            String group = todo.removeFirst();

            done.add( group );
            try
            {
                // logger.info( "Grabbing constituents of: '%s'", group );

                ProxyViewRequest req = new ProxyViewRequest( config, View.GROUP_STORES );
                req.setFullRangeForBaseKey( group );

                List<ArtifactStore> stores = couch.getViewListing( req, ArtifactStore.class );

                if ( stores != null )
                {
                    for ( ArtifactStore store : stores )
                    {
                        if ( store == null )
                        {
                            continue;
                        }

                        // logger.info( "Found constituent: '%s'", store.getKey() );
                        if ( store instanceof Group )
                        {
                            if ( !done.contains( store.getName() ) )
                            {
                                todo.addLast( store.getName() );
                            }
                        }
                        else
                        {
                            accumStores.add( store );
                        }
                    }
                }
            }
            catch ( CouchDBException e )
            {
                throw new ProxyDataException( "Failed to retrieve stores in group: %s. Reason: %s",
                                              e, groupName, e.getMessage() );
            }
        }
    }

    public Set<Group> getGroupsContaining( final StoreKey repo )
        throws ProxyDataException
    {
        try
        {
            ProxyViewRequest req =
                new ProxyViewRequest( config, View.STORE_GROUPS, repo.toString() );

            List<Group> groups = couch.getViewListing( req, Group.class );

            return new HashSet<Group>( groups );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException(
                                          "Failed to lookup groups containing repository: %s. Reason: %s",
                                          e, repo, e.getMessage() );
        }
    }

    public void storeDeployPoints( final Collection<DeployPoint> deploys )
        throws ProxyDataException
    {
        try
        {
            couch.store( deploys, false, false );
            fireStoreEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, deploys );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to update %d deploy-stores. Reason: %s", e,
                                          deploys.size(), e.getMessage() );
        }
    }

    public boolean storeDeployPoint( final DeployPoint deploy )
        throws ProxyDataException
    {
        return storeDeployPoint( deploy, false );
    }

    public boolean storeDeployPoint( final DeployPoint deploy, final boolean skipIfExists )
        throws ProxyDataException
    {
        try
        {
            boolean result = couch.store( deploy, skipIfExists );

            fireStoreEvent( skipIfExists ? ProxyManagerUpdateType.ADD
                            : ProxyManagerUpdateType.ADD_OR_UPDATE, deploy );

            userMgr.createPermissions( StoreType.deploy_point.name(), deploy.getName(),
                                       Permission.ADMIN, Permission.READ, Permission.CREATE );

            return result;
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException(
                                          "Failed to store deploy-store configuration: %s. Reason: %s",
                                          e, deploy.getName(), e.getMessage() );
        }
        catch ( UserDataException e )
        {
            throw new ProxyDataException(
                                          "Failed to create permissions for deploy-store: %s. Reason: %s",
                                          e, deploy.getName(), e.getMessage() );
        }
    }

    public void storeRepositories( final Collection<Repository> repos )
        throws ProxyDataException
    {
        try
        {
            couch.store( repos, false, false );
            fireStoreEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, repos );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to update %d repositories. Reason: %s", e,
                                          repos.size(), e.getMessage() );
        }
    }

    public boolean storeRepository( final Repository proxy )
        throws ProxyDataException
    {
        return storeRepository( proxy, false );
    }

    public boolean storeRepository( final Repository repository, final boolean skipIfExists )
        throws ProxyDataException
    {
        try
        {
            boolean result = couch.store( repository, skipIfExists );

            fireStoreEvent( skipIfExists ? ProxyManagerUpdateType.ADD
                            : ProxyManagerUpdateType.ADD_OR_UPDATE, repository );

            userMgr.createPermissions( StoreType.repository.name(), repository.getName(),
                                       Permission.ADMIN, Permission.READ );

            return result;
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException(
                                          "Failed to store repository configuration: %s. Reason: %s",
                                          e, repository.getName(), e.getMessage() );
        }
        catch ( UserDataException e )
        {
            throw new ProxyDataException(
                                          "Failed to create permissions for repository: %s. Reason: %s",
                                          e, repository.getName(), e.getMessage() );
        }
    }

    public void storeGroups( final Collection<Group> groups )
        throws ProxyDataException
    {
        try
        {
            couch.store( groups, false, false );
            fireStoreEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, groups );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to update %d repository groups. Reason: %s", e,
                                          groups.size(), e.getMessage() );
        }
    }

    public boolean storeGroup( final Group group )
        throws ProxyDataException
    {
        return storeGroup( group, false );
    }

    public boolean storeGroup( final Group group, final boolean skipIfExists )
        throws ProxyDataException
    {
        try
        {
            Set<StoreKey> missing = new HashSet<StoreKey>();
            for ( StoreKey repo : group.getConstituents() )
            {
                if ( !couch.exists( new CouchDocRef( repo.toString() ) ) )
                {
                    missing.add( repo );
                }
            }

            if ( !missing.isEmpty() )
            {
                throw new ProxyDataException(
                                              "Invalid repository-group configuration: %s. Reason: One or more constituent repositories are missing: %s",
                                              group.getName(), new JoinString( ", ", missing ) );
            }

            boolean result = couch.store( group, skipIfExists );

            fireStoreEvent( skipIfExists ? ProxyManagerUpdateType.ADD
                            : ProxyManagerUpdateType.ADD_OR_UPDATE, group );

            userMgr.createPermissions( StoreType.group.name(), group.getName(), Permission.ADMIN,
                                       Permission.READ );

            return result;
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to store group configuration: %s. Reason: %s", e,
                                          group.getName(), e.getMessage() );
        }
        catch ( UserDataException e )
        {
            throw new ProxyDataException( "Failed to create permissions for group: %s. Reason: %s",
                                          e, group.getName(), e.getMessage() );
        }
    }

    public void deleteDeployPoint( final DeployPoint deploy )
        throws ProxyDataException
    {
        try
        {
            couch.delete( deploy );
            fireDeleteEvent( ProxyManagerDeleteEvent.Type.DEPLOY_POINT, deploy.getName() );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException(
                                          "Failed to delete deploy-store configuration: %s. Reason: %s",
                                          e, deploy.getName(), e.getMessage() );
        }
    }

    public void deleteDeployPoint( final String name )
        throws ProxyDataException
    {
        try
        {
            couch.delete( new CouchDocRef( namespaceId( StoreType.deploy_point.name(), name ) ) );
            fireDeleteEvent( ProxyManagerDeleteEvent.Type.DEPLOY_POINT, name );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException(
                                          "Failed to delete deploy-store configuration: %s. Reason: %s",
                                          e, name, e.getMessage() );
        }
    }

    public void deleteRepository( final Repository repo )
        throws ProxyDataException
    {
        try
        {
            couch.delete( repo );
            fireDeleteEvent( ProxyManagerDeleteEvent.Type.REPOSITORY, repo.getName() );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to delete proxy configuration: %s. Reason: %s",
                                          e, repo.getName(), e.getMessage() );
        }
    }

    public void deleteRepository( final String name )
        throws ProxyDataException
    {
        try
        {
            couch.delete( new CouchDocRef( namespaceId( StoreType.repository.name(), name ) ) );
            fireDeleteEvent( ProxyManagerDeleteEvent.Type.REPOSITORY, name );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to delete proxy configuration: %s. Reason: %s",
                                          e, name, e.getMessage() );
        }
    }

    public void deleteGroup( final Group group )
        throws ProxyDataException
    {
        try
        {
            couch.delete( group );
            fireDeleteEvent( ProxyManagerDeleteEvent.Type.GROUP, group.getName() );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to delete group configuration: %s. Reason: %s",
                                          e, group.getName(), e.getMessage() );
        }
    }

    public void deleteGroup( final String name )
        throws ProxyDataException
    {
        try
        {
            couch.delete( new CouchDocRef( namespaceId( StoreType.group.name(), name ) ) );
            fireDeleteEvent( ProxyManagerDeleteEvent.Type.GROUP, name );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to delete group configuration: %s. Reason: %s",
                                          e, name, e.getMessage() );
        }
    }

    public void install()
        throws ProxyDataException
    {
        ProxyAppDescription description = new ProxyAppDescription();
        CouchApp app = new CouchApp( ProxyAppDescription.APP_NAME, description );

        try
        {
            if ( couch.dbExists() )
            {
                // static in Couch, so safe to forcibly reload.
                couch.delete( app );
            }

            couch.initialize( description );

            userMgr.install();
            userMgr.setupAdminInformation();

            userMgr.storePermission( new Permission( StoreType.repository.name(), Permission.ADMIN ) );
            userMgr.storePermission( new Permission( StoreType.group.name(), Permission.ADMIN ) );
            userMgr.storePermission( new Permission( StoreType.repository.name(), Permission.READ ) );
            userMgr.storePermission( new Permission( StoreType.group.name(), Permission.READ ) );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException(
                                          "Failed to initialize proxy-management database: %s (application: %s). Reason: %s",
                                          e, couchConfig.getDatabaseUrl(),
                                          description.getAppName(), e.getMessage() );
        }
        catch ( UserDataException e )
        {
            throw new ProxyDataException(
                                          "Failed to initialize admin user/privilege information in proxy-management database: %s. Reason: %s",
                                          e, couchConfig.getDatabaseUrl(), e.getMessage() );
        }
    }

    private void fireDeleteEvent( final ProxyManagerDeleteEvent.Type type, final String... names )
    {
        if ( delEvent != null )
        {
            delEvent.fire( new ProxyManagerDeleteEvent( type, names ) );
        }
    }

    private void fireStoreEvent( final ProxyManagerUpdateType type, final Repository... repos )
    {
        if ( storeEvent != null )
        {
            storeEvent.fire( new ArtifactStoreUpdateEvent( type, repos ) );
        }
    }

    @SuppressWarnings( "unused" )
    private void fireDeleteEvent( final ProxyManagerDeleteEvent.Type type,
                                  final Collection<String> names )
    {
        if ( delEvent != null )
        {
            delEvent.fire( new ProxyManagerDeleteEvent( type, names ) );
        }
    }

    @SuppressWarnings( "unchecked" )
    private void fireStoreEvent( final ProxyManagerUpdateType type,
                                 final Collection<? extends ArtifactStore> stores )
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

}
