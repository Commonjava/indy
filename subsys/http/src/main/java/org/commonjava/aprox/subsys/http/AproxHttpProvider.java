package org.commonjava.aprox.subsys.http;

import static org.commonjava.aprox.util.LocationUtils.toLocation;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.Repository;
import org.commonjava.maven.galley.auth.AttributePasswordManager;
import org.commonjava.maven.galley.spi.auth.PasswordManager;
import org.commonjava.maven.galley.transport.htcli.Http;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;
import org.commonjava.maven.galley.transport.htcli.model.HttpLocation;

public class AproxHttpProvider
{

    private HttpImpl http;

    private PasswordManager passwordManager;

    @PostConstruct
    public void setup()
    {
        passwordManager = new AttributePasswordManager();
        http = new HttpImpl( passwordManager );
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

    public void bindRepositoryCredentialsTo( final Repository repository, final HttpRequest request )
    {
        http.bindCredentialsTo( (HttpLocation) toLocation( repository ), request );

        if ( repository.getProxyHost() != null )
        {
            //            logger.info( "Using proxy: %s:%s for repository: %s", repository.getProxyHost(),
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
               .setParameter( FileManager.HTTP_PARAM_REPO, repository );
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
