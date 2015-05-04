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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.maven.galley.auth.AttributePasswordManager;
import org.commonjava.maven.galley.spi.auth.PasswordManager;
import org.commonjava.maven.galley.transport.htcli.Http;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;
import org.commonjava.maven.galley.transport.htcli.model.HttpLocation;

@ApplicationScoped
public class AproxHttpProvider
{

    private HttpImpl http;

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

    @Produces
    @Default
    @Singleton
    public HttpClient getClient()
    {
        return http.getClient();
    }

    public void bindRepositoryCredentialsTo( final RemoteRepository repository, final HttpRequest request )
    {
        http.bindCredentialsTo( (HttpLocation) toLocation( repository ), request );

        if ( repository.getProxyHost() != null )
        {
            //            logger.info( "Using proxy: {}:{} for repository: {}", repository.getProxyHost(),
            //                         repository.getProxyPort() < 1 ? 80 : repository.getProxyPort(), repository.getName() );

            final int proxyPort = repository.getProxyPort();
            HttpHost proxy;
            if ( proxyPort < 1 )
            {
                proxy = new HttpHost( repository.getProxyHost(), -1, "http" );
            }
            else
            {
                proxy = new HttpHost( repository.getProxyHost(), repository.getProxyPort(), "http" );
            }

            request.getParams()
                   .setParameter( ConnRoutePNames.DEFAULT_PROXY, proxy );
        }

        request.getParams()
               .setParameter( DownloadManager.HTTP_PARAM_REPO, repository );
    }

    public void clearRepositoryCredentials()
    {
        http.clearAllBoundCredentials();
    }

    public void closeConnection()
    {
        http.closeConnection();
    }

}
