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
package org.commonjava.aprox.client.core.module;

import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.client.core.util.UrlUtils;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.dto.StoreListingDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

public class AproxStoresClientModule
    extends AproxClientModule
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public <T extends ArtifactStore> T create( final T value, final String changelog, final Class<T> type )
        throws AproxClientException
    {
        value.setMetadata( ArtifactStore.METADATA_CHANGELOG, changelog );
        return http.postWithResponse( UrlUtils.buildUrl( "admin", value.getKey()
                                                                       .getType()
                                                                       .singularEndpointName() ),
                                      value, type );
    }

    public boolean exists( final StoreType type, final String name )
        throws AproxClientException
    {
        return http.exists( UrlUtils.buildUrl( "admin", type.singularEndpointName(), name ) );
    }

    public void delete( final StoreType type, final String name, final String changelog )
        throws AproxClientException
    {
        http.deleteWithChangelog( UrlUtils.buildUrl( "admin", type.singularEndpointName(), name ), changelog );
    }

    public boolean update( final ArtifactStore store, final String changelog )
        throws AproxClientException
    {
        store.setMetadata( ArtifactStore.METADATA_CHANGELOG, changelog );
        return http.put( UrlUtils.buildUrl( "admin", store.getKey()
                                                                           .getType()
                                                                           .singularEndpointName(), store.getName() ),
                         store );
    }

    public <T extends ArtifactStore> T load( final StoreType type, final String name, final Class<T> cls )
        throws AproxClientException
    {
        return http.get( UrlUtils.buildUrl( "admin", type.singularEndpointName(), name ), cls );
    }

    public StoreListingDTO<HostedRepository> listHostedRepositories()
        throws AproxClientException
    {
        return http.get( UrlUtils.buildUrl( "admin", StoreType.hosted.singularEndpointName() ),
                         new TypeReference<StoreListingDTO<HostedRepository>>()
                         {
                         } );
    }

    public StoreListingDTO<RemoteRepository> listRemoteRepositories()
        throws AproxClientException
    {
        return http.get( UrlUtils.buildUrl( "admin", StoreType.remote.singularEndpointName() ),
                         new TypeReference<StoreListingDTO<RemoteRepository>>()
                         {
                         } );
    }

    public StoreListingDTO<Group> listGroups()
        throws AproxClientException
    {
        return http.get( UrlUtils.buildUrl( "admin", StoreType.group.singularEndpointName() ),
                         new TypeReference<StoreListingDTO<Group>>()
                         {
                         } );
    }

}
