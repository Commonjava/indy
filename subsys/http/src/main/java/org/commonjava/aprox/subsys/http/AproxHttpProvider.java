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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.subsys.http.util.AproxSiteConfigLookup;
import org.commonjava.maven.galley.spi.auth.PasswordManager;
import org.commonjava.maven.galley.transport.htcli.Http;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.INTERNAL.util.HttpUtils;
import org.commonjava.util.jhttpc.JHttpCException;
import org.commonjava.util.jhttpc.auth.AttributePasswordManager;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@ApplicationScoped
public class AproxHttpProvider
{

    private HttpFactory httpFactory;

    private Http http;

    @Inject
    private AproxSiteConfigLookup siteConfigLookup;

    private PasswordManager passwordManager;

    protected AproxHttpProvider()
    {
    }

    public AproxHttpProvider( AproxSiteConfigLookup siteConfigLookup )
    {
        this.siteConfigLookup = siteConfigLookup;
        setup();
    }

    @PostConstruct
    public void setup()
    {
        passwordManager = new org.commonjava.maven.galley.auth.AttributePasswordManager();
        http = new HttpImpl( passwordManager );
        httpFactory = new HttpFactory( new AttributePasswordManager(siteConfigLookup) );
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
    public Http getHttp()
    {
        return http;
    }

    public HttpClientContext createContext()
            throws AproxHttpException
    {
        try
        {
            return httpFactory.createContext();
        }
        catch ( JHttpCException e )
        {
            throw new AproxHttpException( "Failed to create http client context: %s", e, e.getMessage() );
        }
    }

    public HttpClientContext createContext( final RemoteRepository repository )
            throws AproxHttpException
    {
        try
        {
            return httpFactory.createContext( siteConfigLookup.toSiteConfig( repository ) );
        }
        catch ( JHttpCException e )
        {
            throw new AproxHttpException( "Failed to create http client context for remote repository: %s. Reason: %s", e, repository.getName(), e.getMessage() );
        }
    }

    public CloseableHttpClient createClient()
            throws AproxHttpException
    {
        try
        {
            return httpFactory.createClient();
        }
        catch ( JHttpCException e )
        {
            throw new AproxHttpException( "Failed to create http client: %s", e, e.getMessage() );
        }
    }

    public CloseableHttpClient createClient( final RemoteRepository repository )
            throws AproxHttpException
    {
        try
        {
            return httpFactory.createClient( siteConfigLookup.toSiteConfig( repository ) );
        }
        catch ( JHttpCException e )
        {
            throw new AproxHttpException( "Failed to create http client for remote repository: %s. Reason: %s", e, repository.getName(), e.getMessage() );
        }
    }

    public void cleanup( final CloseableHttpClient client, final HttpUriRequest request,
                         final CloseableHttpResponse response )
    {
        HttpUtils.cleanupResources( client, request, response );
    }

}
