package org.commonjava.aprox.client.core;

import java.io.IOException;

import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.stats.AProxVersioning;

public class AProx
{

    private final AproxClientHttp http;

    public AProx( final String baseUrl )
    {
        this.http = new AproxClientHttp( baseUrl );
    }

    public AProx( final String baseUrl, final AproxObjectMapper mapper )
    {
        this.http = new AproxClientHttp( baseUrl, mapper );
    }

    public AProx connect()
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

}
