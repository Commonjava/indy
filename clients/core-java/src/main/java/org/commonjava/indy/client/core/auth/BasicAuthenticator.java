/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.client.core.auth;

import java.net.URL;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;

public class BasicAuthenticator
    extends IndyClientAuthenticator
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
