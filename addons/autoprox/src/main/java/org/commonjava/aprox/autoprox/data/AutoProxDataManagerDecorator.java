package org.commonjava.aprox.autoprox.data;

import static org.commonjava.couch.util.UrlUtils.buildUrl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.commonjava.aprox.autoprox.conf.AutoDeployConfiguration;
import org.commonjava.aprox.autoprox.conf.AutoGroupConfiguration;
import org.commonjava.aprox.autoprox.conf.AutoProxConfiguration;
import org.commonjava.aprox.autoprox.conf.AutoRepoConfiguration;
import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.ModelFactory;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.util.logging.Logger;

@Decorator
public abstract class AutoProxDataManagerDecorator
    implements StoreDataManager
{

    private static final int MAX_CONNECTIONS = 20;

    private final Logger logger = new Logger( getClass() );

    @Delegate
    @Any
    @Inject
    private StoreDataManager dataManager;

    @Inject
    private AutoProxConfiguration config;

    @Inject
    private ModelFactory modelFactory;

    private HttpClient http;

    @Override
    public Group getGroup( final String name )
        throws ProxyDataException
    {
        Group g = dataManager.getGroup( name );

        if ( !config.isEnabled() )
        {
            logger.info( "AutoProx decorator disabled; returning: %s", g );
            return g;
        }

        logger.info( "AutoProx decorator active" );
        if ( g == null )
        {
            logger.info( "AutoProx: creating repository for: %s", name );
            final Repository proxy = getRepository( name );
            if ( proxy != null )
            {
                final List<StoreKey> keys = new ArrayList<StoreKey>();

                final AutoDeployConfiguration deploy = config.getDeploy();
                if ( deploy.isDeployEnabled() )
                {
                    DeployPoint dp = dataManager.getDeployPoint( name );
                    if ( dp == null )
                    {
                        dp = modelFactory.createDeployPoint( name );

                        dp.setAllowReleases( deploy.isReleasesEnabled() );
                        dp.setAllowSnapshots( deploy.isSnapshotsEnabled() );

                        if ( deploy.getSnapshotTimeoutSeconds() != null )
                        {
                            dp.setSnapshotTimeoutSeconds( deploy.getSnapshotTimeoutSeconds() );
                        }

                        dataManager.storeDeployPoint( dp );
                    }

                    keys.add( dp.getKey() );
                }

                keys.add( proxy.getKey() );

                final AutoGroupConfiguration group = config.getGroup();
                if ( group.getExtraConstituents() != null )
                {
                    keys.addAll( group.getExtraConstituents() );
                }

                g = modelFactory.createGroup( name, keys );
                dataManager.storeGroup( g );
            }
        }

        return g;
    }

    private synchronized boolean checkUrlValidity( final String proxyUrl )
    {
        if ( http == null )
        {
            final ThreadSafeClientConnManager ccm = new ThreadSafeClientConnManager();
            ccm.setMaxTotal( MAX_CONNECTIONS );

            http = new DefaultHttpClient( ccm );
        }

        logger.info( "\n\n\n\n\n[AutoProx] Checking URL: %s", proxyUrl );
        final HttpHead head = new HttpHead( proxyUrl );
        boolean result = false;
        try
        {
            final HttpResponse response = http.execute( head );
            final StatusLine statusLine = response.getStatusLine();
            final int status = statusLine.getStatusCode();
            logger.info( "[AutoProx] HTTP Status: %s", statusLine );
            result = status == HttpStatus.SC_OK;
        }
        catch ( final ClientProtocolException e )
        {
            logger.warn( "[AutoProx] Cannot connect to target repository: '%s'.", proxyUrl );
        }
        catch ( final IOException e )
        {
            logger.warn( "[AutoProx] Cannot connect to target repository: '%s'.", proxyUrl );
        }

        return result;
    }

    @Override
    public Repository getRepository( final String name )
        throws ProxyDataException
    {
        logger.info( "DECORATED" );
        Repository proxy = dataManager.getRepository( name );
        if ( !config.isEnabled() )
        {
            logger.info( "AutoProx decorator disabled; returning: %s", proxy );
            return proxy;
        }

        logger.info( "AutoProx decorator active" );
        if ( proxy == null )
        {
            logger.info( "AutoProx: creating repository for: %s", name );
            String proxyUrl;
            try
            {
                proxyUrl = buildUrl( config.getRepo()
                                           .getBaseUrl(), name );
            }
            catch ( final MalformedURLException e )
            {
                throw new ProxyDataException(
                                              "Cannot build proxy URL for autoprox target: '%s' and base-URL: '%s'. Reason: %s",
                                              e, name, config.getRepo()
                                                             .getBaseUrl(), e.getMessage() );
            }

            if ( !checkUrlValidity( proxyUrl ) )
            {
                logger.warn( "Invalid repository URL: %s", proxyUrl );
                return null;
            }

            if ( proxy == null )
            {
                final AutoRepoConfiguration repo = config.getRepo();

                proxy = modelFactory.createRepository( name, proxyUrl );
                proxy.setPassthrough( repo.isPassthroughEnabled() );
                if ( repo.getTimeoutSeconds() != null )
                {
                    proxy.setTimeoutSeconds( repo.getTimeoutSeconds() );
                }

                if ( repo.getCacheTimeoutSeconds() != null )
                {
                    proxy.setCacheTimeoutSeconds( repo.getCacheTimeoutSeconds() );
                }
                dataManager.storeRepository( proxy );
            }
        }

        return proxy;
    }
}
