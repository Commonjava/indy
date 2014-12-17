package org.commonjava.aprox.client.core;

public abstract class AproxClientModule
{

    protected AproxClientHttp http;

    protected void setup( final AproxClientHttp http )
    {
        this.http = http;
    }

    protected AproxClientHttp getHttp()
    {
        return http;
    }

    @Override
    public final int hashCode()
    {
        return 13 * getClass().hashCode();
    }

    @Override
    public final boolean equals( final Object other )
    {
        return other == this || getClass().equals( other.getClass() );
    }

}
