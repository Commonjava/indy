/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.db.common;

import org.commonjava.indy.data.ArtifactStoreQuery;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.PackageTypes;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.util.UrlInfo;
import org.commonjava.o11yphant.metrics.annotation.Measure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.commonjava.indy.model.core.StoreType.group;

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
// TODO: Eventually, it should probably be an error if packageType isn't set explicitly
@SuppressWarnings( "unchecked" )
public class DefaultArtifactStoreQuery<T extends ArtifactStore>
        implements ArtifactStoreQuery<T>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final StoreDataManager dataManager;

    private String packageType = MavenPackageTypeDescriptor.MAVEN_PKG_KEY;

    private Set<StoreType> types;

    private Boolean enabled;

    public DefaultArtifactStoreQuery( StoreDataManager dataManager )
    {
        logger.debug( "CREATE new default store query with data manager only" );
        this.dataManager = dataManager;
    }

    @SuppressWarnings( "unused" )
    private DefaultArtifactStoreQuery( final StoreDataManager dataManager, final String packageType,
                                       final Boolean enabled, final Class<T> storeCls )
    {
        logger.debug( "CREATE new default store query with params (internal?)" );

        this.dataManager = dataManager;
        this.packageType = packageType;
        this.enabled = enabled;
        storeType( storeCls );
    }

//    @Override
//    public ArtifactStoreQuery<T> rewrap( final StoreDataManager manager )
//    {
//        this.dataManager = manager;
//        return this;
//    }

    @Override
    public <C extends ArtifactStore> DefaultArtifactStoreQuery<C> storeType( Class<C> storeCls )
    {
        if ( RemoteRepository.class.equals( storeCls ) )
        {
            this.types = Collections.singleton( StoreType.remote );
        }
        else if ( HostedRepository.class.equals( storeCls ) )
        {
            this.types = Collections.singleton( StoreType.hosted );
        }
        else
        {
            this.types = Collections.singleton( group );
        }

        return (DefaultArtifactStoreQuery<C>) this;
    }

    @Override
    public DefaultArtifactStoreQuery<T> storeTypes( StoreType... types )
    {
        this.types = new HashSet<>( Arrays.asList( types ) );
        return this;
    }

    @Override
    public ArtifactStoreQuery<T> concreteStores()
    {
        return storeTypes( StoreType.remote, StoreType.hosted );
    }

    @Override
    public ArtifactStoreQuery<T> enabledState( Boolean enabled )
    {
        this.enabled = enabled;
        return this;
    }

//    @Override
//    public boolean isEmpty()
//    {
//        return this.dataManager.isEmpty();
//    }

    @Override
    @Measure
    public List<T> getAll()
            throws IndyDataException
    {
        return stream().collect( Collectors.toList() );
    }

    @Measure
    private Stream<T> stream()
            throws IndyDataException
    {
        return stream( store -> true );
    }

    @Measure
    private Stream<T> stream( Predicate<ArtifactStore> filter )
            throws IndyDataException
    {
        /* @formatter:off */
        return dataManager.streamArtifactStores().filter( ( store ) ->
        {
            logger.debug( "Checking whether {} is included in stream...", store.getKey() );

            // Tricky condition here: The flag in the store we're checking is true when DISABLED, while the
            // condition we're checking against in this query is true when it's ENABLED. If the two flags equal on another
            // that actually means they DISAGREE about the state vs. desired state of the store.
            if ( enabled != null && enabled == store.isDisabled() )
            {
                logger.debug( "Rejected. Store is {}, and we're only looking for enabled state of: {}", store.isDisabled(), enabled );
                return false;
            }

            if ( packageType != null && !packageType.equals( store.getPackageType() ) )
            {
                logger.debug( "Rejected. Store package type is: {}, and we're only looking for package type of: {}", store.getPackageType(), packageType );
                return false;
            }

            if ( types != null && !types.contains( store.getType() ) )
            {
                logger.debug( "Rejected. Store is of type: {}, and we're only looking for: {}", store.getType(), types );
                return false;
            }

            if ( filter != null && !filter.test( store ))
            {
                logger.debug( "Rejected. Additional filtering failed for store: {}", store.getKey() );
                return false;
            }

            logger.debug( "Store accepted for stream: {}", store.getKey() );
            return true;
        } ).map( store -> (T) store );
        /* @formatter:on */
    }

    @Override
    @Measure
    public List<T> getAll( Predicate<ArtifactStore> filter )
            throws IndyDataException
    {
        return stream( filter ).collect( Collectors.toList() );
    }

    @Override
    @Measure
    public List<T> getAllByDefaultPackageTypes()
            throws IndyDataException
    {
        List<T> result = new ArrayList<>();
        Set<String> defaults = PackageTypes.getPackageTypes();
        for ( String packageType : defaults )
        {
            this.packageType = packageType;
            result.addAll( stream().collect( Collectors.toList() ) );
        }
        return result;
    }

    @Override
    @Measure
    public T getByName( String name )
            throws IndyDataException
    {
        return stream( store -> name.equals( store.getName() ) ).findFirst().orElse( null );
    }

//    @Override
//    public boolean containsByName( String name )
//            throws IndyDataException
//    {
//        return getByName( name ) != null;
//    }

    @Override
    @Measure
    public Set<Group> getGroupsContaining( StoreKey storeKey )
    {
        return getGroupsContaining( storeKey, Boolean.TRUE );
    }

    @Override
    @Measure
    public Set<Group> getGroupsContaining( StoreKey storeKey, Boolean enabled )
    {
        return getAllGroups( storeKey.getPackageType(), enabled ).stream().filter( g -> g.getConstituents().contains( storeKey ) ).collect( Collectors.toSet() );
    }

    @Override
    @Measure
    public List<RemoteRepository> getRemoteRepositoryByUrl( String packageType, String url )
    {
        return getRemoteRepositoryByUrl( packageType, url, Boolean.TRUE );
    }

    @Override
    @Measure
    public List<RemoteRepository> getRemoteRepositoryByUrl( String packageType, String url, Boolean enabled )
    {
        /*
           This filter does these things:
             * First compare ip, if ip same, and the path(without last slash) same too, the repo is found
             * If ip not same, then compare the url without scheme and last slash (if has) to find the repo
         */
        List<RemoteRepository> result = emptyList();
        UrlInfo temp;
        try
        {
            temp = new UrlInfo( url );
        }
        catch ( Exception error )
        {
            logger.warn( "Failed to find repository, url: '{}'. Reason: {}", url, error.getMessage() );
            return result;
        }

        final UrlInfo urlInfo = temp;

        // first try to find the remote repo by urlWithNoSchemeAndLastSlash
        final List<RemoteRepository> remoteRepos = getAllRemoteRepositories( packageType, enabled );
        result = remoteRepos.stream().filter( store -> {

            final String targetUrl = store.getUrl();
            UrlInfo targetUrlInfo;
            try
            {
                targetUrlInfo = new UrlInfo( targetUrl );
            }
            catch ( Exception error )
            {
                logger.warn( "Invalid repository, store: {}, url: '{}'. Reason: {}", store.getKey(), targetUrl,
                             error.getMessage() );
                return false;
            }

            if ( urlInfo.getUrlWithNoSchemeAndLastSlash().equals( targetUrlInfo.getUrlWithNoSchemeAndLastSlash() )
                    && urlInfo.getProtocol().equals( targetUrlInfo.getProtocol() ) )
            {
                logger.debug( "Repository found because of same host, url is {}, store key is {}", url,
                              store.getKey() );
                return true;
            }

            return false;
        } ).collect( Collectors.toList() );

        if ( result.isEmpty() )
        {
            // ...if not found by hostname try to search by IP
            /* @formatter:off */
            result = remoteRepos.stream().filter( store -> {

                final String targetUrl = store.getUrl();
                UrlInfo targetUrlInfo;
                try
                {
                    targetUrlInfo = new UrlInfo( targetUrl );
                }
                catch ( Exception error )
                {
                    logger.warn( "Invalid repository, store: {}, url: '{}'. Reason: {}", store.getKey(), targetUrl, error.getMessage() );
                    return false;
                }

                String ipForUrl = null;
                String ipForTargetUrl = null;
                try
                {
                    ipForUrl = urlInfo.getIpForUrl();
                    ipForTargetUrl = targetUrlInfo.getIpForUrl();
                    if ( ipForUrl != null && ipForUrl.equals( ipForTargetUrl )
                            && urlInfo.getPort() == targetUrlInfo.getPort()
                            && urlInfo.getFileWithNoLastSlash().equals( targetUrlInfo.getFileWithNoLastSlash() ) )
                    {
                        logger.debug( "Repository found because of same ip, url is {}, store key is {}", url,
                                      store.getKey() );
                        return true;
                    }
                }
                catch ( UnknownHostException ue )
                {
                    logger.warn( "Failed to filter remote: {}, ip fetch error: {}.", store.getKey(), ue.getMessage() );
                }

                logger.debug( "ip not same: ip for url:{}-{}; ip for searching repo: {}-{}", url, ipForUrl,
                              store.getKey(), ipForTargetUrl );

            return false;
        } ).collect(Collectors.toList());
            /* @formatter:on */
        }

        return result;
    }

    @Override
    @Measure
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( final String packageType, final String groupName )
                    throws IndyDataException
    {
        return getOrderedConcreteStoresInGroup( packageType, groupName, Boolean.TRUE );
    }

    @Override
    @Measure
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( final String packageType, final String groupName, final Boolean enabled )
            throws IndyDataException
    {
        logger.trace( "START: default store-query ordered-concrete-stores-in-group" );
        try
        {
            return getGroupOrdering( packageType, groupName, enabled, false, true );
        }
        finally
        {
            logger.trace( "END: default store-query ordered-concrete-stores-in-group" );
        }
    }

    @Override
    @Measure
    public List<ArtifactStore> getOrderedStoresInGroup( final String packageType, final String groupName )
                    throws IndyDataException
    {
        return getOrderedStoresInGroup( packageType, groupName, Boolean.TRUE );
    }

    @Override
    @Measure
    public List<ArtifactStore> getOrderedStoresInGroup( final String packageType, final String groupName, final Boolean enabled )
            throws IndyDataException
    {
        return getGroupOrdering( packageType, groupName, enabled, true, false );
    }

    @Override
    @Measure
    public Set<Group> getGroupsAffectedBy( StoreKey... keys )
            throws IndyDataException
    {
        return getGroupsAffectedBy( Arrays.asList( keys ) );
    }

    @Override
    @Measure
    public Set<Group> getGroupsAffectedBy( Collection<StoreKey> keys )
            throws IndyDataException
    {
        return dataManager.affectedBy( keys );
    }

    @Override
    public RemoteRepository getRemoteRepository( final String packageType, final String name )
            throws IndyDataException
    {
        return (RemoteRepository) dataManager.getArtifactStore( new StoreKey( packageType, StoreType.remote, name ) );
    }

    @Override
    public HostedRepository getHostedRepository( final String packageType, final String name )
            throws IndyDataException
    {
        return (HostedRepository) dataManager.getArtifactStore( new StoreKey( packageType, StoreType.hosted, name ) );
    }

    @Override
    public Group getGroup( final String packageType, final String name )
            throws IndyDataException
    {
        return (Group) dataManager.getArtifactStore( new StoreKey( packageType, group, name ) );
    }

    @Override
    public DefaultArtifactStoreQuery<T> noPackageType()
    {
        packageType = null;
        return this;
    }

    @Override
    public List<RemoteRepository> getAllRemoteRepositories( String packageType )
    {
        return getAllRemoteRepositories( packageType, Boolean.TRUE );
    }

    @Override
    public List<RemoteRepository> getAllRemoteRepositories( String packageType, Boolean enabled )
    {
        return dataManager.getArtifactStoresByPkgAndType( packageType, StoreType.remote )
                          .stream()
                          .filter( item -> enabled.equals( !item.isDisabled() ) )
                          .map( item -> (RemoteRepository) item )
                          .collect( Collectors.toList() );
    }

    @Override
    public List<HostedRepository> getAllHostedRepositories( String packageType )
    {
        return getAllHostedRepositories( packageType, Boolean.TRUE );
    }

    @Override
    public List<HostedRepository> getAllHostedRepositories( String packageType, Boolean enabled )
    {
        return dataManager.getArtifactStoresByPkgAndType( packageType, StoreType.hosted )
                          .stream()
                          .filter( item -> enabled.equals( !item.isDisabled() ) )
                          .map( item -> (HostedRepository) item )
                          .collect( Collectors.toList() );
    }

    @Override
    public List<Group> getAllGroups( String packageType )
    {
        return getAllGroups( packageType, Boolean.TRUE );
    }

    @Override
    public List<Group> getAllGroups( String packageType, Boolean enabled )
    {
        return dataManager.getArtifactStoresByPkgAndType( packageType, group )
                          .stream()
                          .filter( item -> enabled.equals( !item.isDisabled() ) )
                          .map( item -> (Group) item )
                          .collect( Collectors.toList() );
    }

    private List<ArtifactStore> getGroupOrdering( final String packageType, final String groupName, final Boolean enabled,
                                                  final boolean includeGroups, final boolean recurseGroups )
            throws IndyDataException
    {
        if ( packageType == null )
        {
            throw new IndyDataException( "packageType must be set on the query before calling this method!" );
        }

        final Group master = (Group) dataManager.getArtifactStore( new StoreKey( packageType, group, groupName ) );
        if ( master == null )
        {
            return emptyList();
        }

        final List<ArtifactStore> result = new ArrayList<>();

        return getMembersOrdering( master, enabled, result, includeGroups, recurseGroups );

    }

    private List<ArtifactStore> getMembersOrdering(final Group groupRepo, final Boolean enabled, final List<ArtifactStore> result,
                                                   final boolean includeGroups, final boolean recurseGroups) throws IndyDataException
    {

        if ( groupRepo == null || groupRepo.isDisabled() && enabled )
        {
            return result;
        }

        Set<StoreKey> seen = result.stream().map( ArtifactStore::getKey ).collect( Collectors.toSet() );
        AtomicReference<IndyDataException> errorRef = new AtomicReference<>();

        List<StoreKey> members = new ArrayList<>( groupRepo.getConstituents() );
        if ( includeGroups )
        {
            result.add( groupRepo );
        }

        members.forEach(( key ) ->
        {
            if (!seen.contains( key ))
            {
                seen.add( key );
                final StoreType type = key.getType();
                try
                {
                    if ( recurseGroups && type == group )
                    {
                        // if we're here, we're definitely recursing groups...
                        Group group = (Group) dataManager.getArtifactStore(key);
                        getMembersOrdering( group, enabled, result, includeGroups, recurseGroups );
                    }
                    else
                    {
                        final ArtifactStore store = dataManager.getArtifactStore( key );
                        if ( store != null && ( store.isDisabled() != enabled ) )
                        {
                            result.add( store );
                        }
                    }
                }
                catch ( IndyDataException e )
                {
                    errorRef.set( e );
                }
            }
        });

        IndyDataException error = errorRef.get();
        if ( error != null )
        {
            throw error;
        }

        return result;
    }

}
