package org.commonjava.aprox.client.core.auth;

import java.net.URL;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;
import org.commonjava.aprox.client.core.AproxClientException;

public abstract class AproxClientAuthenticator
{

    public HttpClientContext decoratePrototypeContext( final URL url, final HttpClientContext ctx )
        throws AproxClientException
    {
        return ctx;
    }

    public HttpClientBuilder decorateClientBuilder( final URL url, final HttpClientBuilder builder )
        throws AproxClientException
    {
        return builder;
    }

}
