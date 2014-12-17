package org.commonjava.aprox.client.core;

import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreType;

public class Stores
{
    private final AproxClientHttp http;

    Stores( final AproxClientHttp http )
    {
        this.http = http;
    }

    public <T extends ArtifactStore> T create( final T value, final Class<T> type )
        throws AproxClientException
    {
        return http.postWithResponse( String.format( "/admin/%s", value.getKey()
                                                                       .getType()
                                                                       .singularEndpointName() ), value, type );
    }

    public boolean exists( final StoreType type, final String name )
        throws AproxClientException
    {
        return http.exists( String.format( "/admin/%s/%s", type.singularEndpointName(), name ) );
    }

    public void delete( final StoreType type, final String name )
        throws AproxClientException
    {
        http.delete( String.format( "/admin/%s/%s", type.singularEndpointName(), name ) );
    }

    public boolean update( final ArtifactStore store )
        throws AproxClientException
    {
        return http.put( String.format( "/admin/%s/%s", store.getKey()
                                                             .getType()
                                                             .singularEndpointName(), store.getName() ), store );
    }

    public ArtifactStore load( final StoreType type, final String name )
        throws AproxClientException
    {
        return http.get( String.format( "/admin/%s/%s", type.singularEndpointName(), name ), type.getStoreClass() );
    }

}
