package org.commonjava.aprox.infinispan.inject;

public enum AproxCaches
{
    DATA( "aprox-data" );

    private final String cacheName;

    private AproxCaches( final String cn )
    {
        cacheName = cn;
    }

    public String cacheName()
    {
        return cacheName;
    }

}
