package org.commonjava.aprox.client.core;

final class ErrorHolder
{
    private AproxClientException error;

    public AproxClientException getError()
    {
        return error;
    }

    public void setError( final AproxClientException error )
    {
        this.error = error;
    }

    public boolean hasError()
    {
        return error != null;
    }

}
