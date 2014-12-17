package org.commonjava.aprox.client.core;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.stats.AProxVersioning;

public class Aprox
{

    private final AproxClientHttp http;

    private Set<AproxClientModule> moduleRegistry;

    public Aprox( final String baseUrl, final AproxClientModule... modules )
    {
        this.http = new AproxClientHttp( baseUrl );
        this.moduleRegistry = new HashSet<>();
        for ( final AproxClientModule module : modules )
        {
            module.setup( http );
            moduleRegistry.add( module );
        }
    }

    public Aprox( final String baseUrl, final AproxObjectMapper mapper, final AproxClientModule... modules )
    {
        this.http = new AproxClientHttp( baseUrl, mapper );
    }

    public Aprox connect()
    {
        http.connect();
        return this;
    }

    public void close()
        throws IOException
    {
        http.close();
    }

    public AProxVersioning getVersionInfo()
        throws AproxClientException
    {
        return http.get( "/stats/version-info", AProxVersioning.class );
    }

    public Stores stores()
    {
        return new Stores( http );
    }

    public <T extends AproxClientModule> T module( final Class<T> type )
        throws AproxClientException
    {
        for ( final AproxClientModule module : moduleRegistry )
        {
            if ( type.isInstance( module ) )
            {
                return type.cast( module );
            }
        }

        throw new AproxClientException( "Module not found: %s.", type.getName() );
    }

}
