package org.commonjava.aprox.subsys.http;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.subsys.http.util.RepoSSLSocketFactory;
import org.commonjava.aprox.subsys.http.util.TLRepositoryCredentialsProvider;
import org.commonjava.util.logging.Logger;

@Singleton
public class AproxHttp
{
    private final Logger logger = new Logger( getClass() );

    private RepoSSLSocketFactory socketFactory;

    private TLRepositoryCredentialsProvider credProvider;

    private DefaultHttpClient client;

    public static AproxHttp getInstance()
    {
        final AproxHttp http = new AproxHttp();
        http.setup();

        return http;
    }

    @PostConstruct
    protected void setup()
    {
        final ThreadSafeClientConnManager ccm = new ThreadSafeClientConnManager();

        // TODO: Make this configurable
        ccm.setMaxTotal( 20 );

        try
        {
            socketFactory = new RepoSSLSocketFactory();

            final SchemeRegistry registry = ccm.getSchemeRegistry();
            registry.register( new Scheme( "https", 443, socketFactory ) );
        }
        catch ( final KeyManagementException e )
        {
            logger.error( "Failed to setup SSLSocketFactory. SSL mutual authentication will not be available!\nError: %s",
                          e, e.getMessage() );
        }
        catch ( final UnrecoverableKeyException e )
        {
            logger.error( "Failed to setup SSLSocketFactory. SSL mutual authentication will not be available!\nError: %s",
                          e, e.getMessage() );
        }
        catch ( final NoSuchAlgorithmException e )
        {
            logger.error( "Failed to setup SSLSocketFactory. SSL mutual authentication will not be available!\nError: %s",
                          e, e.getMessage() );
        }
        catch ( final KeyStoreException e )
        {
            logger.error( "Failed to setup SSLSocketFactory. SSL mutual authentication will not be available!\nError: %s",
                          e, e.getMessage() );
        }

        credProvider = new TLRepositoryCredentialsProvider();

        final DefaultHttpClient hc = new DefaultHttpClient( ccm );
        hc.setCredentialsProvider( credProvider );

        client = hc;
    }

    public HttpClient getClient()
    {
        return client;
    }

    public void bindRepositoryCredentials( final Repository repository )
    {
        credProvider.bind( repository );
    }

    public void clearRepositoryCredentials()
    {
        credProvider.clear();
    }

    public void closeConnection()
    {
        client.getConnectionManager()
              .closeExpiredConnections();

        client.getConnectionManager()
              .closeIdleConnections( 2, TimeUnit.SECONDS );
    }

}
