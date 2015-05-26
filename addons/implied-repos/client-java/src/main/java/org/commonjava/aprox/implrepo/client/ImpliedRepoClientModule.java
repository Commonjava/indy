package org.commonjava.aprox.implrepo.client;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.aprox.client.core.Aprox;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientHttp;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.implrepo.ImpliedReposException;
import org.commonjava.aprox.implrepo.data.ImpliedRepoMetadataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImpliedRepoClientModule
    extends AproxClientModule
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private ImpliedRepoMetadataManager metadataManager;

    @Override
    protected void setup( final Aprox client, final AproxClientHttp http )
    {
        super.setup( client, http );
        this.metadataManager = new ImpliedRepoMetadataManager( getObjectMapper() );
    }

    public List<StoreKey> getStoresImpliedBy( final StoreType type, final String name )
        throws AproxClientException
    {
        final ArtifactStore store = getClient().stores()
                                               .load( type, name, ArtifactStore.class );
        if ( store == null )
        {
            return null;
        }

        return getStoresImpliedBy( store );
    }

    public List<StoreKey> getStoresImpliedBy( final ArtifactStore store )
        throws AproxClientException
    {
        try
        {
            return metadataManager.getStoresImpliedBy( store );
        }
        catch ( final ImpliedReposException e )
        {
            throw new AproxClientException( "Failed to retrieve implied-store metadata: %s", e, e.getMessage() );
        }
    }

    public List<StoreKey> getStoresImplying( final StoreType type, final String name )
        throws AproxClientException
    {
        final ArtifactStore store = getClient().stores()
                                               .load( type, name, ArtifactStore.class );
        if ( store == null )
        {
            return null;
        }

        return getStoresImplying( store );
    }

    public List<StoreKey> getStoresImplying( final ArtifactStore store )
        throws AproxClientException
    {
        try
        {
            return metadataManager.getStoresImplying( store );
        }
        catch ( final ImpliedReposException e )
        {
            throw new AproxClientException( "Failed to retrieve implied-store metadata: %s", e, e.getMessage() );
        }
    }

    public void setStoresImpliedBy( final ArtifactStore store, final List<StoreKey> implied, final String changelog )
        throws AproxClientException
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
                throw new AproxClientException( "No such store: %s. Cannot add to the implied-store list for: %s",
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
            throw new AproxClientException( "Failed to set implied-store metadata: %s", e, e.getMessage() );
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
