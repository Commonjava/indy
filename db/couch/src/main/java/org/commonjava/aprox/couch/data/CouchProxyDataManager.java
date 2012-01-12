/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.couch.data;

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
import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.ModelFactory;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.aprox.core.model.io.StoreKeySerializer;
import org.commonjava.aprox.couch.data.ProxyAppDescription.View;
import org.commonjava.aprox.couch.model.ArtifactStoreDoc;
import org.commonjava.aprox.couch.model.DeployPointDoc;
import org.commonjava.aprox.couch.model.GroupDoc;
import org.commonjava.aprox.couch.model.RepositoryDoc;
import org.commonjava.aprox.couch.model.io.StoreDocDeserializer;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.db.CouchDBException;
import org.commonjava.couch.db.CouchManager;
import org.commonjava.couch.io.Serializer;
import org.commonjava.couch.model.CouchApp;
import org.commonjava.couch.model.CouchDocRef;
import org.commonjava.couch.util.JoinString;

@Singleton
public class CouchProxyDataManager
    implements ProxyDataManager
{
    // private final Logger logger = new Logger( getClass() );

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

    @Inject
    private ModelFactory modelFactory;

    public CouchProxyDataManager()
    {
    }

    public CouchProxyDataManager( final ProxyConfiguration config, final CouchDBConfiguration couchConfig,
                                  final CouchManager couch, final Serializer serializer, final ModelFactory modelFactory )
    {
        this.config = config;
        this.couchConfig = couchConfig;
        this.couch = couch;
        this.serializer = serializer;
        this.modelFactory = modelFactory;

        registerSerializationAdapters();
    }

    @PostConstruct
    protected void registerSerializationAdapters()
    {
        serializer.registerSerializationAdapters( new StoreKeySerializer(), new StoreDocDeserializer(), modelFactory );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#getDeployPoint(java.lang.String)
     */
    @Override
    public DeployPointDoc getDeployPoint( final String name )
        throws ProxyDataException
    {
        try
        {
            return couch.getDocument( new CouchDocRef( namespaceId( StoreType.deploy_point.name(), name ) ),
                                      DeployPointDoc.class );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve deploy-store: %s. Reason: %s", e, name, e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#getRepository(java.lang.String)
     */
    @Override
    public Repository getRepository( final String name )
        throws ProxyDataException
    {
        try
        {
            return couch.getDocument( new CouchDocRef( namespaceId( StoreType.repository.name(), name ) ),
                                      RepositoryDoc.class );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve repository: %s. Reason: %s", e, name, e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#getGroup(java.lang.String)
     */
    @Override
    public GroupDoc getGroup( final String name )
        throws ProxyDataException
    {
        try
        {
            return couch.getDocument( new CouchDocRef( namespaceId( StoreType.group.name(), name ) ), GroupDoc.class );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve group: %s. Reason: %s", e, name, e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#getAllGroups()
     */
    @Override
    public List<Group> getAllGroups()
        throws ProxyDataException
    {
        try
        {
            return new ArrayList<Group>( couch.getViewListing( new ProxyViewRequest( config, View.ALL_GROUPS ),
                                                               GroupDoc.class ) );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve group listing. Reason: %s", e, e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#getAllRepositories()
     */
    @Override
    public List<Repository> getAllRepositories()
        throws ProxyDataException
    {
        try
        {
            return new ArrayList<Repository>(
                                              couch.getViewListing( new ProxyViewRequest( config, View.ALL_REPOSITORIES ),
                                                                    RepositoryDoc.class ) );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve repository listing. Reason: %s", e, e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#getAllDeployPoints()
     */
    @Override
    public List<DeployPoint> getAllDeployPoints()
        throws ProxyDataException
    {
        try
        {
            return new ArrayList<DeployPoint>( couch.getViewListing( new ProxyViewRequest( config,
                                                                                           View.ALL_REPOSITORIES ),
                                                                     DeployPointDoc.class ) );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve deploy-store listing. Reason: %s", e, e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#getOrderedConcreteStoresInGroup(java.lang.String)
     */
    @Override
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
        throws ProxyDataException
    {
        final List<ArtifactStore> result = new ArrayList<ArtifactStore>();
        findOrderedConcreteStores( groupName, result );

        return result;
    }

    private void findOrderedConcreteStores( final String groupName, final List<ArtifactStore> accumStores )
        throws ProxyDataException
    {
        final LinkedList<String> todo = new LinkedList<String>();
        final Set<String> done = new HashSet<String>();

        todo.addLast( groupName );
        while ( !todo.isEmpty() )
        {
            final String group = todo.removeFirst();

            done.add( group );
            try
            {
                // logger.info( "Grabbing constituents of: '%s'", group );

                final ProxyViewRequest req = new ProxyViewRequest( config, View.GROUP_STORES );
                req.setFullRangeForBaseKey( group );

                final List<ArtifactStoreDoc> stores = couch.getViewListing( req, ArtifactStoreDoc.class );

                if ( stores != null )
                {
                    for ( final ArtifactStore store : stores )
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
            catch ( final CouchDBException e )
            {
                throw new ProxyDataException( "Failed to retrieve stores in group: %s. Reason: %s", e, groupName,
                                              e.getMessage() );
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.commonjava.aprox.core.data.ProxyDataManager#getGroupsContaining(org.commonjava.aprox.core.model.StoreKey)
     */
    @Override
    public Set<Group> getGroupsContaining( final StoreKey repo )
        throws ProxyDataException
    {
        try
        {
            final ProxyViewRequest req = new ProxyViewRequest( config, View.STORE_GROUPS, repo.toString() );

            final List<? extends Group> groups = couch.getViewListing( req, GroupDoc.class );

            return new HashSet<Group>( groups );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to lookup groups containing repository: %s. Reason: %s", e, repo,
                                          e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#storeDeployPoints(java.util.Collection)
     */
    @Override
    public void storeDeployPoints( final Collection<? extends DeployPoint> deploys )
        throws ProxyDataException
    {
        try
        {
            couch.store( convertDocuments( deploys ), false, false );
            fireStoreEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, deploys );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to update %d deploy-stores. Reason: %s", e, deploys.size(),
                                          e.getMessage() );
        }
    }

    private Collection<? extends ArtifactStoreDoc> convertDocuments( final Collection<? extends ArtifactStore> stores )
    {
        final List<ArtifactStoreDoc> docs = new ArrayList<ArtifactStoreDoc>( stores.size() );
        for ( final ArtifactStore store : stores )
        {
            if ( store instanceof ArtifactStoreDoc )
            {
                docs.add( (ArtifactStoreDoc) store );
            }
            else
            {
                docs.add( (ArtifactStoreDoc) modelFactory.convertModel( store ) );
            }
        }

        return docs;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.commonjava.aprox.core.data.ProxyDataManager#storeDeployPoint(org.commonjava.aprox.core.model.DeployPoint)
     */
    @Override
    public boolean storeDeployPoint( final DeployPoint deploy )
        throws ProxyDataException
    {
        return storeDeployPoint( deploy, false );
    }

    /*
     * (non-Javadoc)
     * @see
     * org.commonjava.aprox.core.data.ProxyDataManager#storeDeployPoint(org.commonjava.aprox.core.model.DeployPoint,
     * boolean)
     */
    @Override
    public boolean storeDeployPoint( final DeployPoint deploy, final boolean skipIfExists )
        throws ProxyDataException
    {
        try
        {
            final boolean result = couch.store( (ArtifactStoreDoc) modelFactory.convertModel( deploy ), skipIfExists );

            fireStoreEvent( skipIfExists ? ProxyManagerUpdateType.ADD : ProxyManagerUpdateType.ADD_OR_UPDATE, deploy );

            return result;
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to store deploy-store configuration: %s. Reason: %s", e,
                                          deploy.getName(), e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#storeRepositories(java.util.Collection)
     */
    @Override
    public void storeRepositories( final Collection<? extends Repository> repos )
        throws ProxyDataException
    {
        try
        {
            couch.store( convertDocuments( repos ), false, false );
            fireStoreEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, repos );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to update %d repositories. Reason: %s", e, repos.size(),
                                          e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#storeRepository(org.commonjava.aprox.core.model.Repository)
     */
    @Override
    public boolean storeRepository( final Repository proxy )
        throws ProxyDataException
    {
        return storeRepository( proxy, false );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#storeRepository(org.commonjava.aprox.core.model.Repository,
     * boolean)
     */
    @Override
    public boolean storeRepository( final Repository repository, final boolean skipIfExists )
        throws ProxyDataException
    {
        try
        {
            final boolean result =
                couch.store( (ArtifactStoreDoc) modelFactory.convertModel( repository ), skipIfExists );

            fireStoreEvent( skipIfExists ? ProxyManagerUpdateType.ADD : ProxyManagerUpdateType.ADD_OR_UPDATE,
                            repository );

            return result;
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to store repository configuration: %s. Reason: %s", e,
                                          repository.getName(), e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#storeGroups(java.util.Collection)
     */
    @Override
    public void storeGroups( final Collection<? extends Group> groups )
        throws ProxyDataException
    {
        try
        {
            couch.store( convertDocuments( groups ), false, false );
            fireStoreEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, groups );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to update %d repository groups. Reason: %s", e, groups.size(),
                                          e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#storeGroup(org.commonjava.aprox.core.model.Group)
     */
    @Override
    public boolean storeGroup( final Group group )
        throws ProxyDataException
    {
        return storeGroup( group, false );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#storeGroup(org.commonjava.aprox.core.model.Group, boolean)
     */
    @Override
    public boolean storeGroup( final Group group, final boolean skipIfExists )
        throws ProxyDataException
    {
        try
        {
            final Set<StoreKey> missing = new HashSet<StoreKey>();
            for ( final StoreKey repo : group.getConstituents() )
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

            final boolean result = couch.store( (ArtifactStoreDoc) modelFactory.convertModel( group ), skipIfExists );

            fireStoreEvent( skipIfExists ? ProxyManagerUpdateType.ADD : ProxyManagerUpdateType.ADD_OR_UPDATE, group );

            return result;
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to store group configuration: %s. Reason: %s", e, group.getName(),
                                          e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.commonjava.aprox.core.data.ProxyDataManager#deleteDeployPoint(org.commonjava.aprox.core.model.DeployPoint)
     */
    @Override
    public void deleteDeployPoint( final DeployPoint deploy )
        throws ProxyDataException
    {
        try
        {
            couch.delete( (ArtifactStoreDoc) modelFactory.convertModel( deploy ) );
            fireDeleteEvent( StoreType.deploy_point, deploy.getName() );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to delete deploy-store configuration: %s. Reason: %s", e,
                                          deploy.getName(), e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#deleteDeployPoint(java.lang.String)
     */
    @Override
    public void deleteDeployPoint( final String name )
        throws ProxyDataException
    {
        try
        {
            couch.delete( new CouchDocRef( namespaceId( StoreType.deploy_point.name(), name ) ) );
            fireDeleteEvent( StoreType.deploy_point, name );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to delete deploy-store configuration: %s. Reason: %s", e, name,
                                          e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#deleteRepository(org.commonjava.aprox.core.model.Repository)
     */
    @Override
    public void deleteRepository( final Repository repo )
        throws ProxyDataException
    {
        try
        {
            couch.delete( (ArtifactStoreDoc) modelFactory.convertModel( repo ) );
            fireDeleteEvent( StoreType.repository, repo.getName() );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to delete proxy configuration: %s. Reason: %s", e, repo.getName(),
                                          e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#deleteRepository(java.lang.String)
     */
    @Override
    public void deleteRepository( final String name )
        throws ProxyDataException
    {
        try
        {
            couch.delete( new CouchDocRef( namespaceId( StoreType.repository.name(), name ) ) );
            fireDeleteEvent( StoreType.repository, name );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to delete proxy configuration: %s. Reason: %s", e, name,
                                          e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#deleteGroup(org.commonjava.aprox.core.model.Group)
     */
    @Override
    public void deleteGroup( final Group group )
        throws ProxyDataException
    {
        try
        {
            couch.delete( (ArtifactStoreDoc) modelFactory.convertModel( group ) );
            fireDeleteEvent( StoreType.group, group.getName() );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to delete group configuration: %s. Reason: %s", e, group.getName(),
                                          e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#deleteGroup(java.lang.String)
     */
    @Override
    public void deleteGroup( final String name )
        throws ProxyDataException
    {
        try
        {
            couch.delete( new CouchDocRef( namespaceId( StoreType.group.name(), name ) ) );
            fireDeleteEvent( StoreType.group, name );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to delete group configuration: %s. Reason: %s", e, name,
                                          e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#install()
     */
    @Override
    public void install()
        throws ProxyDataException
    {
        final ProxyAppDescription description = new ProxyAppDescription();
        final CouchApp app = new CouchApp( ProxyAppDescription.APP_NAME, description );

        try
        {
            if ( couch.dbExists() )
            {
                // static in Couch, so safe to forcibly reload.
                couch.delete( app );
            }

            couch.initialize( description );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException(
                                          "Failed to initialize proxy-management database: %s (application: %s). Reason: %s",
                                          e, couchConfig.getDatabaseUrl(), description.getAppName(), e.getMessage() );
        }
    }

    private void fireDeleteEvent( final StoreType type, final String... names )
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
    private void fireDeleteEvent( final StoreType type, final Collection<String> names )
    {
        if ( delEvent != null )
        {
            delEvent.fire( new ProxyManagerDeleteEvent( type, names ) );
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

}
