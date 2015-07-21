package org.commonjava.aprox.client.core.auth;

import java.net.URL;
import java.util.Collections;

import org.apache.http.Header;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.commonjava.aprox.client.core.AproxClientException;

public class OAuth20BearerTokenAuthenticator
    extends AproxClientAuthenticator
{

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_FORMAT = "Bearer %s";

    private final String token;

    public OAuth20BearerTokenAuthenticator( final String token )
    {
        this.token = token;
    }

    @Override
    public HttpClientBuilder decorateClientBuilder( final URL url, final HttpClientBuilder builder )
        throws AproxClientException
    {
        final Header header = new BasicHeader( AUTHORIZATION_HEADER, String.format( BEARER_FORMAT, token ) );
        return builder.setDefaultHeaders( Collections.<Header> singleton( header ) );
    }

}
