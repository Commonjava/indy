package org.commonjava.aprox.rest.util.inject;

public enum AproxGridCaches
{

    DATA( "aprox-storage-data" ), METADATA( "aprox-storage-metadata" );

    private final String cacheName;

    private AproxGridCaches( final String cn )
    {
        cacheName = cn;
    }

    public String cacheName()
    {
        return cacheName;
    }

}
