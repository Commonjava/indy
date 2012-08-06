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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.core.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.core.change.event.ProxyManagerDeleteEvent;
import org.commonjava.aprox.core.change.event.ProxyManagerUpdateType;
import org.commonjava.aprox.core.conf.AproxConfiguration;
import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.aprox.core.model.io.StoreKeySerializer;
import org.commonjava.aprox.couch.data.AproxAppDescription.View;
import org.commonjava.aprox.couch.model.AbstractArtifactStoreDoc;
import org.commonjava.aprox.couch.model.ArtifactStoreDoc;
import org.commonjava.aprox.couch.model.DeployPointDoc;
import org.commonjava.aprox.couch.model.GroupDoc;
import org.commonjava.aprox.couch.model.RepositoryDoc;
import org.commonjava.aprox.couch.model.convert.ModelVersionConverter;
import org.commonjava.aprox.couch.model.io.StoreDocDeserializer;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.db.CouchDBException;
import org.commonjava.couch.db.CouchManager;
import org.commonjava.couch.io.Serializer;
import org.commonjava.couch.model.CouchApp;
import org.commonjava.couch.model.CouchDocRef;
import org.commonjava.couch.util.JoinString;
import org.commonjava.util.logging.Logger;

@Singleton
public class CouchStoreDataManager
    implements StoreDataManager
{
    private final Logger logger = new Logger( getClass() );

    @Inject
    @AproxData
    private CouchManager couch;

    @Inject
    private AproxConfiguration config;

    @Inject
    @AproxData
    private CouchDBConfiguration couchConfig;

    @Inject
    private Event<ArtifactStoreUpdateEvent> storeEvent;

    @Inject
    private Event<ProxyManagerDeleteEvent> delEvent;

    @Inject
    private Serializer serializer;

    public CouchStoreDataManager()
    {
    }

    public CouchStoreDataManager( final AproxConfiguration config, final CouchDBConfiguration couchConfig,
                                  final CouchManager couch, final Serializer serializer )
    {
        this.config = config;
        this.couchConfig = couchConfig;
        this.couch = couch;
        this.serializer = serializer;

        registerSerializationAdapters();
    }

    @PostConstruct
    protected void registerSerializationAdapters()
    {
        serializer.registerSerializationAdapters( new StoreKeySerializer(), new StoreDocDeserializer(),
                                                  new ModelVersionConverter() );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.data.ProxyDataManager#getDeployPoint(java.lang.String)
     */
    @Override
    public DeployPoint getDeployPoint( final String name )
        throws ProxyDataException
    {
        try
        {
            final DeployPointDoc doc =
                couch.getDocument( new CouchDocRef( namespaceId( StoreType.deploy_point.name(), name ) ),
                                   DeployPointDoc.class );

            return doc == null ? null : doc.exportStore();
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
            final RepositoryDoc doc =
                couch.getDocument( new CouchDocRef( namespaceId( StoreType.repository.name(), name ) ),
                                   RepositoryDoc.class );

            return doc == null ? null : doc.exportStore();
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
    public Group getGroup( final String name )
        throws ProxyDataException
    {
        try
        {
            final GroupDoc doc =
                couch.getDocument( new CouchDocRef( namespaceId( StoreType.group.name(), name ) ), GroupDoc.class );

            return doc == null ? null : doc.exportStore();
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
            final List<GroupDoc> docs =
                new ArrayList<GroupDoc>( couch.getViewListing( new AproxViewRequest( config, View.ALL_GROUPS ),
                                                               GroupDoc.class ) );

            return exportList( docs );
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
            final List<RepositoryDoc> docs =
                new ArrayList<RepositoryDoc>(
                                              couch.getViewListing( new AproxViewRequest( config, View.ALL_REPOSITORIES ),
                                                                    RepositoryDoc.class ) );

            return exportList( docs );
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
            final List<DeployPointDoc> docs =
                new ArrayList<DeployPointDoc>( couch.getViewListing( new AproxViewRequest( config,
                                                                                           View.ALL_DEPLOY_POINTS ),
                                                                     DeployPointDoc.class ) );

            return exportList( docs );
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
        final Set<String> done = new HashSet<String>();

        findOrderedConcreteStores( groupName, result, done );

        return result;
    }

    @SuppressWarnings( "rawtypes" )
    private void findOrderedConcreteStores( final String group, final List<ArtifactStore> accumStores,
                                            final Set<String> done )
        throws ProxyDataException
    {
        done.add( group );
        try
        {
            logger.info( "Grabbing constituents of: '%s'", group );

            final AproxViewRequest req = new AproxViewRequest( config, View.GROUP_STORES );
            req.setFullRangeForBaseKey( group );

            final List<ArtifactStoreDoc> stores = couch.getViewListing( req, ArtifactStoreDoc.class );

            if ( stores != null )
            {
                for ( final ArtifactStoreDoc storeDoc : stores )
                {
                    if ( storeDoc == null )
                    {
                        continue;
                    }

                    final ArtifactStore store = storeDoc.exportStore();
                    logger.info( "Found constituent: '%s'", store.getKey() );
                    if ( !done.contains( store.getName() ) )
                    {
                        if ( storeDoc instanceof GroupDoc )
                        {
                            logger.info( "Traversing: '%s'", store.getKey() );
                            findOrderedConcreteStores( store.getName(), accumStores, done );
                        }
                        else
                        {
                            logger.info( "Adding: '%s'", store.getKey() );
                            accumStores.add( store );
                        }
                    }
                    else
                    {
                        logger.info( "Already added: %s", store.getKey() );
                    }
                }
            }
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve stores in group: %s. Reason: %s", e, group,
                                          e.getMessage() );
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
            final AproxViewRequest req = new AproxViewRequest( config, View.STORE_GROUPS, repo.toString() );

            final List<GroupDoc> groupDocs = couch.getViewListing( req, GroupDoc.class );
            return new HashSet<Group>( exportList( groupDocs ) );
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
    public void storeDeployPoints( final Collection<DeployPoint> deploys )
        throws ProxyDataException
    {
        try
        {
            couch.store( wrapCollection( deploys ), false, false );
            fireStoreEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, deploys );
        }
        catch ( final CouchDBException e )
        {
            throw new ProxyDataException( "Failed to update %d deploy-stores. Reason: %s", e, deploys.size(),
                                          e.getMessage() );
        }
    }

    @SuppressWarnings( "rawtypes" )
    private <T extends ArtifactStore> Collection<ArtifactStoreDoc> wrapCollection( final Collection<T> stores )
    {
        if ( stores == null )
        {
            return Collections.emptyList();
        }

        final List<ArtifactStoreDoc> docs = new ArrayList<ArtifactStoreDoc>( stores.size() );
        for ( final T store : stores )
        {
            docs.add( wrapOne( store ) );
        }

        return docs;
    }

    @SuppressWarnings( "rawtypes" )
    private ArtifactStoreDoc wrapOne( final ArtifactStore store )
    {
        if ( StoreType.deploy_point == store.getDoctype() )
        {
            return new DeployPointDoc( (DeployPoint) store );
        }
        else if ( StoreType.group == store.getDoctype() )
        {
            return new GroupDoc( (Group) store );
        }
        else if ( StoreType.repository == store.getDoctype() )
        {
            return new RepositoryDoc( (Repository) store );
        }

        throw new IllegalStateException( "Detected ArtifactStore with missing/invalid doctype! Store: " + store );
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
            @SuppressWarnings( "rawtypes" )
            ArtifactStoreDoc doc = wrapOne( deploy );
            if ( !skipIfExists && couch.exists( doc ) )
            {
                final DeployPoint toUpdate = getDeployPoint( deploy.getName() );
                toUpdate.setAllowReleases( deploy.isAllowReleases() );
                toUpdate.setAllowSnapshots( deploy.isAllowSnapshots() );
                toUpdate.setSnapshotTimeoutSeconds( deploy.getSnapshotTimeoutSeconds() );

                doc = wrapOne( toUpdate );
            }

            final boolean result = couch.store( doc, skipIfExists );

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
    public void storeRepositories( final Collection<Repository> repos )
        throws ProxyDataException
    {
        try
        {
            couch.store( wrapCollection( repos ), false, false );
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
            @SuppressWarnings( "rawtypes" )
            ArtifactStoreDoc doc = wrapOne( repository );
            if ( !skipIfExists && couch.exists( doc ) )
            {
                Repository toUpdate = getRepository( repository.getName() );
                if ( toUpdate == null )
                {
                    toUpdate = repository;
                }
                else
                {
                    toUpdate.setUrl( repository.getUrl() );
                    toUpdate.setUser( repository.getUser() );
                    toUpdate.setPassword( repository.getPassword() );
                    toUpdate.setTimeoutSeconds( repository.getTimeoutSeconds() );
                    toUpdate.setPassthrough( repository.isPassthrough() );
                    toUpdate.setCacheTimeoutSeconds( repository.getCacheTimeoutSeconds() );
                    toUpdate.setKeyCertPem( repository.getKeyCertPem() );
                    toUpdate.setKeyPassword( repository.getKeyPassword() );
                    toUpdate.setProxyHost( repository.getProxyHost() );
                    toUpdate.setProxyPassword( repository.getProxyPassword() );
                    toUpdate.setProxyPort( repository.getProxyPort() );
                    toUpdate.setProxyUser( repository.getProxyUser() );
                    toUpdate.setServerCertPem( repository.getServerCertPem() );
                }

                doc = wrapOne( toUpdate );
            }

            final boolean result = couch.store( doc, skipIfExists );

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
    public void storeGroups( final Collection<Group> groups )
        throws ProxyDataException
    {
        try
        {
            couch.store( wrapCollection( groups ), false, false );
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
            @SuppressWarnings( "rawtypes" )
            ArtifactStoreDoc doc = wrapOne( group );
            if ( !skipIfExists && couch.exists( doc ) )
            {
                final Group toUpdate = getGroup( group.getName() );
                toUpdate.setConstituents( group.getConstituents() );

                doc = wrapOne( toUpdate );
            }

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

            final boolean result = couch.store( doc, skipIfExists );

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
            couch.delete( wrapOne( deploy ) );
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
            couch.delete( wrapOne( repo ) );
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
            couch.delete( wrapOne( group ) );
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
        final AproxAppDescription description = new AproxAppDescription();
        final CouchApp app = new CouchApp( AproxAppDescription.APP_NAME, description );

        try
        {
            if ( couch.dbExists() )
            {
                logger.info( "deleting for reinstall: %s", app.getCouchDocId() );

                // static in Couch, so safe to forcibly reload.
                couch.delete( app );
            }

            logger.info( "initializing app: %s", app.getCouchDocId() );
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

    private <T extends ArtifactStore> List<T> exportList( final List<? extends AbstractArtifactStoreDoc<T>> docs )
    {
        if ( docs == null )
        {
            return Collections.emptyList();
        }

        final List<T> results = new ArrayList<T>( docs.size() );
        for ( final AbstractArtifactStoreDoc<T> doc : docs )
        {
            results.add( doc.exportStore() );
        }

        return results;
    }

}
