package org.commonjava.aprox.client.core;

import java.nio.file.Paths;

import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.RemoteRepository;
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
        return http.putWithResponse( "/admin", value, type );
    }

    public boolean exists( final StoreType type, final String name )
        throws AproxClientException
    {
        return http.exists( Paths.get( "/admin", type.singularEndpointName(), name )
                                 .toString() );
    }

    public void delete( final StoreType type, final String name )
        throws AproxClientException
    {
        http.delete( Paths.get( "/admin", type.singularEndpointName(), name )
                                 .toString() );
    }

    public <T extends ArtifactStore> T update( final T store, final Class<T> type )
        throws AproxClientException
    {
        return http.postWithResponse( "/admin", store, type );
    }

    public RemoteRepository load( final StoreType type, final String name )
        throws AproxClientException
    {
        return http.get( Paths.get( "/admin", type.singularEndpointName(), name )
                              .toString(), RemoteRepository.class );
    }

}
