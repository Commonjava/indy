package org.commonjava.aprox.client.core.auth;

import java.net.URL;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;

public class BasicAuthenticator
    extends AproxClientAuthenticator
{

    private final String user;

    private final String pass;

    public BasicAuthenticator( final String user, final String pass )
    {
        this.user = user;
        this.pass = pass;
    }

    @Override
    public HttpClientContext decoratePrototypeContext( final URL url, final HttpClientContext ctx )
    {
        if ( user != null )
        {
            final AuthScope as =
                new AuthScope( url.getHost(), url.getPort() < 0 ? url.getDefaultPort() : url.getPort() );

            final CredentialsProvider credProvider = new BasicCredentialsProvider();
            credProvider.setCredentials( as, new UsernamePasswordCredentials( user, pass ) );
            ctx.setCredentialsProvider( credProvider );
        }

        return ctx;
    }

}
