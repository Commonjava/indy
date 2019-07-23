/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.implrepo.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.indy.implrepo.ImpliedReposException;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;

@Alternative
@Named
public class ImpliedRepoMetadataManager
{

    public static final String IMPLIED_STORES = "implied_stores";

    public static final String IMPLIED_BY_STORES = "implied_by_stores";

    private final IndyObjectMapper mapper;

    public ImpliedRepoMetadataManager( final IndyObjectMapper mapper )
    {
        this.mapper = mapper;
    }

    // TODO: Need to deal with pre-existing implications.
    public void addImpliedMetadata( final ArtifactStore origin, final List<ArtifactStore> implied )
        throws ImpliedReposException
    {
        try
        {
            final List<StoreKey> impliedKeys = new ArrayList<>( implied.size() );
            for ( final ArtifactStore store : implied )
            {
                impliedKeys.add( store.getKey() );
                updateImpliedBy( store, origin );
            }

            origin.setMetadata( IMPLIED_STORES, mapper.writeValueAsString( new ImpliedRemotesWrapper( impliedKeys ) ) );
        }
        catch ( final JsonProcessingException e )
        {
            throw new ImpliedReposException( "Failed to serialize implied stores: %s to JSON: %s. Error: %s", e,
                                             implied, origin.getKey(), e.getMessage() );
        }
    }

    public void updateImpliedBy( final ArtifactStore store, final ArtifactStore origin )
        throws ImpliedReposException
    {
        final String metadata = store.getMetadata( IMPLIED_BY_STORES );

        ImpliedRemotesWrapper wrapper;
        if ( metadata == null )
        {
            wrapper = new ImpliedRemotesWrapper( Collections.singletonList( origin.getKey() ) );
        }
        else
        {
            try
            {
                wrapper = mapper.readValue( metadata, ImpliedRemotesWrapper.class );
                if ( !wrapper.addItem( origin.getKey() ) )
                {
                    // nothing to change; set to null to signal that nothing should change.
                    wrapper = null;
                }
            }
            catch ( final IOException e )
            {
                throw new ImpliedReposException(
                                                 "Failed to de-serialize implied-by stores from: %s\nJSON: %s\nError: %s",
                                                 e, store.getKey(), metadata, e.getMessage() );
            }
        }

        if ( wrapper != null )
        {
            try
            {
                store.setMetadata( IMPLIED_BY_STORES, mapper.writeValueAsString( wrapper ) );
            }
            catch ( final JsonProcessingException e )
            {
                throw new ImpliedReposException( "Failed to serialize implied-by stores to: %s\nJSON: %s\nError: %s",
                                                 e, store.getKey(), metadata, e.getMessage() );
            }
        }

    }

    public List<StoreKey> getStoresImpliedBy( final ArtifactStore origin )
        throws ImpliedReposException
    {
        final String metadata = origin.getMetadata( IMPLIED_STORES );
        if ( metadata == null )
        {
            return null;
        }

        try
        {
            final ImpliedRemotesWrapper wrapper = mapper.readValue( metadata, ImpliedRemotesWrapper.class );
            return wrapper.getItems();
        }
        catch ( final IOException e )
        {
            throw new ImpliedReposException( "Failed to de-serialize implied stores from: %s\nJSON: %s\nError: %s", e,
                                             origin.getKey(), metadata, e.getMessage() );
        }
    }

    public List<StoreKey> getStoresImplying( final ArtifactStore store )
        throws ImpliedReposException
    {
        final String metadata = store.getMetadata( IMPLIED_BY_STORES );
        if ( metadata == null )
        {
            return null;
        }

        try
        {
            final ImpliedRemotesWrapper wrapper = mapper.readValue( metadata, ImpliedRemotesWrapper.class );
            return wrapper.getItems();
        }
        catch ( final IOException e )
        {
            throw new ImpliedReposException( "Failed to de-serialize implied stores from: %s\nJSON: %s\nError: %s", e,
                                             store.getKey(), metadata, e.getMessage() );
        }
    }

    public static final class ImpliedRemotesWrapper
    {
        private List<StoreKey> items;

        public ImpliedRemotesWrapper( final List<StoreKey> items )
        {
            this.items = items;
        }

        boolean addItem( final StoreKey key )
        {
            if ( key == null )
            {
                return false;
            }

            if ( items == null )
            {
                items = new ArrayList<>();
            }

            if ( !items.contains( key ) )
            {
                return items.add( key );
            }

            return false;
        }

        @SuppressWarnings( "unused" )
        ImpliedRemotesWrapper()
        {
        }

        public List<StoreKey> getItems()
        {
            return items;
        }

        @SuppressWarnings( "unused" )
        public void setItems( final List<StoreKey> items )
        {
            this.items = items;
        }
    }

}
