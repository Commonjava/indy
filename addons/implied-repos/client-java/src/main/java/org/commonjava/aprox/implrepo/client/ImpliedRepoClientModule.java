package org.commonjava.aprox.implrepo.client;

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

public class ImpliedRepoClientModule
    extends AproxClientModule
{

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

}
