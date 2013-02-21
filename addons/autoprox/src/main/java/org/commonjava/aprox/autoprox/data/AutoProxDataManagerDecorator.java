package org.commonjava.aprox.autoprox.data;

import static org.commonjava.aprox.util.UrlUtils.buildUrl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.regex.Pattern;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpHead;
import org.commonjava.aprox.autoprox.conf.AutoProxConfiguration;
import org.commonjava.aprox.autoprox.conf.AutoProxModel;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.subsys.http.AproxHttp;
import org.commonjava.util.logging.Logger;

@Decorator
public abstract class AutoProxDataManagerDecorator
    implements StoreDataManager
{

    private static final String REPO_NAME_URL_PATTERN = Pattern.quote( "${name}" );

    private static final String REPO_CONSTITUENT_PLACEHOLDER = "${repository}";

    private static final String DEPLOY_CONSTITUENT_PLACEHOLDER = "${deploy}";

    private final Logger logger = new Logger( getClass() );

    @Delegate
    @Any
    @Inject
    private StoreDataManager dataManager;

    @Inject
    private AutoProxConfiguration config;

    @Inject
    private AutoProxModel autoproxModel;

    @Inject
    private AproxHttp http;

    @Override
    public Group getGroup( final String name )
        throws ProxyDataException
    {
        //        logger.info( "DECORATED (getGroup: %s)", name );
        Group g = dataManager.getGroup( name );

        if ( !config.isEnabled() )
        {
            //            logger.info( "AutoProx decorator disabled; returning: %s", g );
            return g;
        }

        //        logger.info( "AutoProx decorator active" );
        if ( g == null )
        {
            //            logger.info( "AutoProx: creating repository for: %s", name );
            final Repository proxy = getRepository( name );
            if ( proxy != null )
            {
                DeployPoint dp = null;
                if ( config.isDeployEnabled() )
                {
                    dp = dataManager.getDeployPoint( name );

                    if ( dp == null )
                    {
                        dp = new DeployPoint( name );

                        final DeployPoint deployTemplate = autoproxModel.getDeploy();

                        if ( deployTemplate != null )
                        {
                            dp.setAllowReleases( deployTemplate.isAllowReleases() );
                            dp.setAllowSnapshots( deployTemplate.isAllowSnapshots() );
                            dp.setSnapshotTimeoutSeconds( deployTemplate.getSnapshotTimeoutSeconds() );
                        }

                        dataManager.storeDeployPoint( dp );
                    }
                }

                g = new Group( name );

                boolean rFound = false;
                boolean dFound = false;
                final Group groupTemplate = autoproxModel.getGroup();

                if ( groupTemplate != null && groupTemplate.getConstituents() != null )
                {
                    for ( final StoreKey storeKey : groupTemplate.getConstituents() )
                    {
                        if ( storeKey.getType() == StoreType.repository
                            && REPO_CONSTITUENT_PLACEHOLDER.equalsIgnoreCase( storeKey.getName() ) )
                        {
                            g.addConstituent( proxy );
                            rFound = true;
                        }
                        else if ( dp != null && storeKey.getType() == StoreType.deploy_point
                            && DEPLOY_CONSTITUENT_PLACEHOLDER.equalsIgnoreCase( storeKey.getName() ) )
                        {
                            g.addConstituent( dp );
                            dFound = true;
                        }
                        else
                        {
                            g.addConstituent( storeKey );
                        }
                    }
                }

                final List<StoreKey> constituents = g.getConstituents();
                if ( !rFound )
                {
                    constituents.add( 0, proxy.getKey() );
                }

                if ( dp != null && !dFound )
                {
                    constituents.add( 0, dp.getKey() );
                }

                dataManager.storeGroup( g );
            }
        }

        return g;
    }

    private synchronized boolean checkUrlValidity( final Repository repo, final String proxyUrl,
                                                   final String validationPath )
    {
        String url = null;
        try
        {
            url = buildUrl( proxyUrl, validationPath );
        }
        catch ( final MalformedURLException e )
        {
            logger.error( "Failed to construct repository-validation URL from base: %s and path: %s. Reason: %s", e,
                          proxyUrl, validationPath, e.getMessage() );
            return false;
        }

        //        logger.info( "\n\n\n\n\n[AutoProx] Checking URL: %s from:", new Throwable(), url );
        final HttpHead head = new HttpHead( url );

        http.bindRepositoryCredentialsTo( repo, head );

        boolean result = false;
        try
        {
            final HttpResponse response = http.getClient()
                                              .execute( head );
            final StatusLine statusLine = response.getStatusLine();
            final int status = statusLine.getStatusCode();
            //            logger.info( "[AutoProx] HTTP Status: %s", statusLine );
            result = status == HttpStatus.SC_OK;
        }
        catch ( final ClientProtocolException e )
        {
            logger.warn( "[AutoProx] Cannot connect to target repository: '%s'.", url );
        }
        catch ( final IOException e )
        {
            logger.warn( "[AutoProx] Cannot connect to target repository: '%s'.", url );
        }
        finally
        {
            http.clearRepositoryCredentials();
            http.closeConnection();
        }

        return result;
    }

    @Override
    public Repository getRepository( final String name )
        throws ProxyDataException
    {
        //        logger.info( "DECORATED (getRepository: %s)", name );
        Repository repo = dataManager.getRepository( name );
        if ( !config.isEnabled() )
        {
            //            logger.info( "AutoProx decorator disabled; returning: %s", repo );
            return repo;
        }

        //        logger.info( "AutoProx decorator active" );
        if ( repo == null )
        {
            //            logger.info( "AutoProx: creating repository for: %s", name );

            final Repository repoTemplate = autoproxModel.getRepo();
            final String validationPath = autoproxModel.getRepoValidationPath();

            final String url = resolveRepoUrl( repoTemplate.getUrl(), name );

            if ( repo == null )
            {
                repo = new Repository( name, url );

                repo.setCacheTimeoutSeconds( repoTemplate.getCacheTimeoutSeconds() );
                repo.setHost( repoTemplate.getHost() );
                repo.setKeyCertPem( repoTemplate.getKeyCertPem() );
                repo.setKeyPassword( repoTemplate.getKeyPassword() );
                repo.setPassthrough( repoTemplate.isPassthrough() );
                repo.setPassword( repoTemplate.getPassword() );
                repo.setPort( repoTemplate.getPort() );
                repo.setProxyHost( repoTemplate.getProxyHost() );
                repo.setProxyPassword( repoTemplate.getProxyPassword() );
                repo.setProxyPort( repoTemplate.getProxyPort() );
                repo.setProxyUser( repoTemplate.getProxyUser() );
                repo.setServerCertPem( repoTemplate.getServerCertPem() );
                repo.setTimeoutSeconds( repoTemplate.getTimeoutSeconds() );
                repo.setUser( repoTemplate.getUser() );

                if ( !checkUrlValidity( repo, url, validationPath ) )
                {
                    logger.warn( "Invalid repository URL: %s", url );
                    return null;
                }

                dataManager.storeRepository( repo );
            }
        }

        return repo;
    }

    private String resolveRepoUrl( final String src, final String name )
    {
        return src.replaceAll( REPO_NAME_URL_PATTERN, name );
    }

    @Override
    public ArtifactStore getArtifactStore( final StoreKey key )
        throws ProxyDataException
    {
        //        logger.info( "DECORATED (getArtifactStore: %s)", key );
        final StoreType type = key.getType();
        switch ( type )
        {
            case group:
            {
                return getGroup( key.getName() );
            }
            case repository:
            {
                return getRepository( key.getName() );
            }
            default:
            {
                return dataManager.getArtifactStore( key );
            }
        }
    }
}
