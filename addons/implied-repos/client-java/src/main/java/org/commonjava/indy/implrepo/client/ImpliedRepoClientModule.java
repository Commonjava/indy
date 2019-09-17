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
package org.commonjava.indy.implrepo.client;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.indy.client.core.Indy;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientHttp;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.implrepo.ImpliedReposException;
import org.commonjava.indy.implrepo.data.ImpliedRepoMetadataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;

public class ImpliedRepoClientModule
    extends IndyClientModule
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private ImpliedRepoMetadataManager metadataManager;

    @Override
    protected void setup( final Indy client, final IndyClientHttp http )
    {
        super.setup( client, http );
        this.metadataManager = new ImpliedRepoMetadataManager( getObjectMapper() );
    }

    @Deprecated
    public List<StoreKey> getStoresImpliedBy( final StoreType type, final String name )
        throws IndyClientException
    {
        return getStoresImpliedBy( new StoreKey( MAVEN_PKG_KEY, type, name ) );
    }

    public List<StoreKey> getStoresImpliedBy( final StoreKey key )
            throws IndyClientException
    {
        final ArtifactStore store = getClient().stores()
                                               .load( key, ArtifactStore.class );
        if ( store == null )
        {
            return null;
        }

        return getStoresImpliedBy( store );
    }

    public List<StoreKey> getStoresImpliedBy( final ArtifactStore store )
        throws IndyClientException
    {
        try
        {
            return metadataManager.getStoresImpliedBy( store );
        }
        catch ( final ImpliedReposException e )
        {
            throw new IndyClientException( "Failed to retrieve implied-store metadata: %s", e, e.getMessage() );
        }
    }

    @Deprecated
    public List<StoreKey> getStoresImplying( final StoreType type, final String name )
            throws IndyClientException
    {
        return getStoresImplying( new StoreKey( MAVEN_PKG_KEY, type, name ) );
    }

    public List<StoreKey> getStoresImplying( final StoreKey key )
        throws IndyClientException
    {
        final ArtifactStore store = getClient().stores()
                                               .load( key, ArtifactStore.class );
        if ( store == null )
        {
            return null;
        }

        return getStoresImplying( store );
    }

    public List<StoreKey> getStoresImplying( final ArtifactStore store )
        throws IndyClientException
    {
        try
        {
            return metadataManager.getStoresImplying( store );
        }
        catch ( final ImpliedReposException e )
        {
            throw new IndyClientException( "Failed to retrieve implied-store metadata: %s", e, e.getMessage() );
        }
    }

    public void setStoresImpliedBy( final ArtifactStore store, final List<StoreKey> implied, final String changelog )
        throws IndyClientException
    {
        final List<ArtifactStore> stores = new ArrayList<>();
        for ( final StoreKey storeKey : implied )
        {
            final ArtifactStore is =
                getClient().stores()
                           .load( storeKey.getType(), storeKey.getName(), storeKey.getType()
                                                                                  .getStoreClass() );
            if ( is == null )
            {
                throw new IndyClientException( "No such store: %s. Cannot add to the implied-store list for: %s",
                                                storeKey, store.getKey() );
            }

            stores.add( is );
        }

        try
        {
            metadataManager.addImpliedMetadata( store, stores );
        }
        catch ( final ImpliedReposException e )
        {
            throw new IndyClientException( "Failed to set implied-store metadata: %s", e, e.getMessage() );
        }

        stores.add( store );

        for ( final ArtifactStore toSave : stores )
        {
            logger.info( "Updating implied-store metadata in: {} triggered by adding implications to: {}",
                         toSave.getKey(), store.getKey() );

            getClient().stores()
                       .update( toSave, changelog );
        }
    }

}
