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
import java.util.Collections;

import org.apache.http.Header;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.util.jhttpc.JHttpCException;

public class OAuth20BearerTokenAuthenticator
    extends IndyClientAuthenticator
{

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_FORMAT = "Bearer %s";

    private final String token;

    public OAuth20BearerTokenAuthenticator( final String token )
    {
        this.token = token;
    }

    public HttpClientBuilder decorateClientBuilder( final URL url, final HttpClientBuilder builder )
        throws IndyClientException
    {
        try {
            return decorateClientBuilder(builder);
        }
        catch (JHttpCException e)
        {
            throw new IndyClientException( "Create context error: {}", e );
        }
    }

    @Override
    public HttpClientBuilder decorateClientBuilder(HttpClientBuilder builder)
            throws JHttpCException
    {
        final Header header = new BasicHeader( AUTHORIZATION_HEADER, String.format( BEARER_FORMAT, token ) );
        return builder.setDefaultHeaders( Collections.<Header> singleton( header ) );
    }

}
