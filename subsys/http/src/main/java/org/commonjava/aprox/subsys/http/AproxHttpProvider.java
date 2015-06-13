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
package org.commonjava.aprox.subsys.http;

import static org.commonjava.aprox.util.LocationUtils.toLocation;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.maven.galley.auth.AttributePasswordManager;
import org.commonjava.maven.galley.spi.auth.PasswordManager;
import org.commonjava.maven.galley.transport.htcli.Http;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;
import org.commonjava.maven.galley.transport.htcli.model.HttpLocation;

@ApplicationScoped
public class AproxHttpProvider
{

    private Http http;

    private PasswordManager passwordManager;

    protected AproxHttpProvider()
    {
    }

    public AproxHttpProvider( final PasswordManager passwordManager )
    {
        this.passwordManager = passwordManager;
        setup();
    }

    @PostConstruct
    public void setup()
    {
        if ( passwordManager == null )
        {
            passwordManager = new AttributePasswordManager();
        }

        http = new HttpImpl( passwordManager );
    }

    @Produces
    @Default
    @Singleton
    public PasswordManager getPasswordManager()
    {
        return passwordManager;
    }

    @Produces
    @Default
    @Singleton
    public Http getHttpComponent()
    {
        return http;
    }

    public HttpClientContext createContext()
    {
        return http.createContext();
    }

    public HttpClientContext createContext( final RemoteRepository repository )
    {
        return http.createContext( (HttpLocation) toLocation( repository ) );
    }

    public CloseableHttpClient createClient()
        throws IOException
    {
        return http.createClient();
    }

    public CloseableHttpClient createClient( final RemoteRepository validationRepo )
        throws IOException
    {
        return http.createClient( (HttpLocation) toLocation( validationRepo ) );
    }

    public void cleanup( final CloseableHttpClient client, final HttpUriRequest request,
                         final CloseableHttpResponse response )
    {
        http.cleanup( client, request, response );
    }

}
