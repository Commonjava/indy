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
package org.commonjava.indy.client.core.module;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.dto.SimpleBooleanResultDTO;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.commonjava.indy.pkg.PackageTypeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class IndyStoreQueryClientModule
        extends IndyClientModule
{
    public static final String ALL_PACKAGE_TYPES = "_all";

    public static final String STORE_QUERY_BASEPATH = "admin/stores/query";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public <T extends ArtifactStore> StoreListingDTO<T> getAllStores( final String packageType,
                                                                      final Set<StoreType> types,
                                                                      final Boolean enabled )
            throws IndyClientException
    {
        final StringBuilder queryPath = new StringBuilder();
        if ( PackageTypeConstants.isValidPackageType( packageType ) )
        {
            queryPath.append( "?packageType=" ).append( packageType );
        }
        if ( types != null && !types.isEmpty() )
        {
            if ( queryPath.length() <= 0 )
            {
                queryPath.append( "?types=" );

            }
            else
            {
                queryPath.append( "&types=" );
            }
            queryPath.append( types.stream().map( Objects::toString ).collect( Collectors.joining( "," ) ) );
        }
        if ( enabled != null )
        {
            if ( queryPath.length() <= 0 )
            {
                queryPath.append( "?enabled=" ).append( enabled );

            }
            else
            {
                queryPath.append( "&enabled=" ).append( enabled );
            }
        }
        String path = UrlUtils.buildUrl( STORE_QUERY_BASEPATH, "all/" + queryPath );
        return http.get( path, new TypeReference<StoreListingDTO<T>>()
        {
        } );
    }

    public <T extends ArtifactStore> StoreListingDTO<T> getAllByDefaultPkgTypes()
            throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( STORE_QUERY_BASEPATH, "byDefaultPkgTypes" ),
                         new TypeReference<StoreListingDTO<T>>()
                         {
                         } );
    }

    public <T extends ArtifactStore> T getByName( final String name )
            throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( STORE_QUERY_BASEPATH, "byName", name ), new TypeReference<T>()
        {
        } );
    }

    public StoreListingDTO<Group> getGroupContaining( final StoreKey storeKey, final String enabled )
            throws IndyClientException
    {
        if ( storeKey == null )
        {
            throw new IndyClientException( "StoreKey is required!" );
        }
        final StringBuilder queryPath = new StringBuilder( "?storeKey=" + storeKey );

        if ( enabled != null )
        {
            queryPath.append( "&enabled=" ).append( enabled );
        }
        String path = UrlUtils.buildUrl( STORE_QUERY_BASEPATH, "groups/contains" + queryPath );

        return http.get( path, new TypeReference<StoreListingDTO<Group>>()
        {
        } );
    }

    public StoreListingDTO<RemoteRepository> getRemoteRepositoryByUrl( final String packageType, final String url,
                                                                       final String enabled )
            throws IndyClientException
    {
        final String pkgType = PackageTypeConstants.isValidPackageType( packageType ) ?
                packageType :
                PackageTypeConstants.PKG_TYPE_MAVEN;
        try
        {
            URL u = new URL( url );
        }
        catch ( MalformedURLException e )
        {
            throw new IndyClientException( "url {} is not a valid!" );
        }
        final StringBuilder queryPath = new StringBuilder();
        queryPath.append( "?packageType=" ).append( pkgType ).append( "&byUrl=" ).append( url );
        if ( enabled != null )
        {
            queryPath.append( "&enabled=" ).append( enabled );
        }
        return http.get( UrlUtils.buildUrl( STORE_QUERY_BASEPATH, "remotes/" + queryPath ),
                         new TypeReference<StoreListingDTO<RemoteRepository>>()
                         {
                         } );
    }

    public StoreListingDTO<Group> getGroupsAffectedBy( final Set<StoreKey> storeKeys )
            throws IndyClientException
    {
        final String keys = storeKeys.stream().map( StoreKey::toString ).collect( Collectors.joining( "," ) );
        return http.get( UrlUtils.buildUrl( STORE_QUERY_BASEPATH, "affectedBy/?keys=" + keys ),
                         new TypeReference<StoreListingDTO<Group>>()
                         {
                         } );
    }

    public StoreListingDTO<ArtifactStore> getOrderedConcreteStoresInGroup( final String packageType,
                                                                           final String groupName,
                                                                           final String enabled )
            throws IndyClientException
    {
        return getStoresInGroup( packageType, groupName, enabled, "concretes/inGroup/" );
    }

    public StoreListingDTO<ArtifactStore> getOrderedStoresInGroup( final String packageType, final String groupName,
                                                                   final String enabled )
            throws IndyClientException
    {
        return getStoresInGroup( packageType, groupName, enabled, "inGroup/" );
    }

    private StoreListingDTO<ArtifactStore> getStoresInGroup( final String packageType, final String groupName,
                                                             final String enabled, final String apiPath )
            throws IndyClientException
    {
        final String pkgType = PackageTypeConstants.isValidPackageType( packageType ) ?
                packageType :
                PackageTypeConstants.PKG_TYPE_MAVEN;
        if ( StringUtils.isBlank( groupName ) )
        {
            throw new IndyClientException( "group name cannot be empty!" );
        }
        final String storeKey = new StoreKey( packageType, StoreType.group, groupName ).toString();
        final StringBuilder queryPath = new StringBuilder();
        queryPath.append( "?storeKey=" ).append( storeKey );
        if ( enabled != null )
        {
            queryPath.append( "&enabled=" ).append( enabled );
        }
        return http.get( UrlUtils.buildUrl( STORE_QUERY_BASEPATH, apiPath + queryPath ),
                         new TypeReference<StoreListingDTO<ArtifactStore>>()
                         {
                         } );
    }

    public StoreListingDTO<RemoteRepository> getAllRemoteRepositories( final String packageType, final String enabled )
            throws IndyClientException
    {
        return getAllSubStores( packageType, enabled, "remotes/all/" );
    }

    public StoreListingDTO<HostedRepository> getAllHostedRepositories( final String packageType, final String enabled )
            throws IndyClientException
    {
        return getAllSubStores( packageType, enabled, "hosteds/all/" );
    }

    public StoreListingDTO<Group> getAllGroups( final String packageType, final String enabled )
            throws IndyClientException
    {
        return getAllSubStores( packageType, enabled, "groups/all/" );
    }

    private <T extends ArtifactStore> StoreListingDTO<T> getAllSubStores( final String packageType,
                                                                          final String enabled, final String apiPath )
            throws IndyClientException
    {
        final String pkgType = PackageTypeConstants.isValidPackageType( packageType ) ?
                packageType :
                PackageTypeConstants.PKG_TYPE_MAVEN;
        final StringBuilder queryPath = new StringBuilder();
        queryPath.append( "?packageType=" ).append( pkgType );
        if ( enabled != null )
        {
            queryPath.append( "&enabled=" ).append( enabled );
        }
        return http.get( UrlUtils.buildUrl( STORE_QUERY_BASEPATH, apiPath + queryPath ),
                         new TypeReference<StoreListingDTO<T>>()
                         {
                         } );
    }

    public SimpleBooleanResultDTO getStoreEmptyResult()
            throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( STORE_QUERY_BASEPATH, "isEmpty" ),
                         new TypeReference<SimpleBooleanResultDTO>()
                         {
                         } );
    }

}
