/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.subsys.http;

import static org.commonjava.aprox.util.LocationUtils.toLocation;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.model.RemoteRepository;
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

    @Inject
    private ClientConnectionManager connectionManager;

    protected AproxHttpProvider()
    {
    }

    public AproxHttpProvider( final PasswordManager passwordManager, final ClientConnectionManager connectionManager )
    {
        this.passwordManager = passwordManager;
        this.connectionManager = connectionManager;
        setup();
    }

    @PostConstruct
    public void setup()
    {
        if ( passwordManager == null )
        {
            passwordManager = new AttributePasswordManager();
        }

        if ( connectionManager == null )
        {
            connectionManager = new AproxHttpConnectionManager( true );
        }

        http = new HttpImpl( passwordManager, connectionManager );
    }

    @Produces
    public PasswordManager getPasswordManager()
    {
        return passwordManager;
    }

    @Produces
    public Http getHttpComponent()
    {
        return http;
    }

    @Produces
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
