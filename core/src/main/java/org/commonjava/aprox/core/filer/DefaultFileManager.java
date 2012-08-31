/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.core.filer;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.commonjava.aprox.util.UrlUtils.buildUrl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.commonjava.aprox.change.event.FileStorageEvent;
import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.core.filer.httputils.RepoSSLSocketFactory;
import org.commonjava.aprox.core.filer.httputils.TLRepositoryCredentialsProvider;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.io.StorageProvider;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ArtifactPathInfo;
import org.commonjava.util.logging.Logger;

@Singleton
public class DefaultFileManager
    implements FileManager
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private AproxConfiguration config;

    @Inject
    private StorageProvider storage;

    @Inject
    private Event<FileStorageEvent> fileEvent;

    private HttpClient client;

    private final Map<String, Future<StorageItem>> pending = new HashMap<String, Future<StorageItem>>();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private TLRepositoryCredentialsProvider credProvider;

    private RepoSSLSocketFactory socketFactory;

    public DefaultFileManager()
    {
    }

    public DefaultFileManager( final AproxConfiguration config, final StorageProvider storage )
    {
        this.config = config;
        this.storage = storage;
        setup();
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

    @Override
    public StorageItem retrieveFirst( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        StorageItem target = null;

        for ( final ArtifactStore store : stores )
        {
            if ( store == null )
            {
                continue;
            }

            logger.info( "Attempting retrieval of: %s from store: %s", path, store );
            target = retrieve( store, path, true );
            if ( target != null )
            {
                logger.info( "Returning content from file: %s", target );
                return target;
            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#downloadAll(java.util.List, java.lang.String)
     */
    @Override
    public Set<StorageItem> retrieveAll( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        final Set<StorageItem> results = new LinkedHashSet<StorageItem>();

        StorageItem stream = null;
        for ( final ArtifactStore store : stores )
        {
            stream = retrieve( store, path, true );
            if ( stream != null )
            {
                results.add( stream );
            }
        }

        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#download(org.commonjava.aprox.core.model.ArtifactStore,
     * java.lang.String)
     */
    @Override
    public StorageItem retrieve( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        return retrieve( store, path, false );
    }

    private StorageItem retrieve( final ArtifactStore store, final String path, final boolean suppressFailures )
        throws AproxWorkflowException
    {
        StorageItem target = null;
        if ( store instanceof Repository )
        {
            final Repository repo = (Repository) store;
            target = getStorageReference( store, path );

            download( repo, target, suppressFailures );
        }
        else
        {
            target = getStorageReference( store, path );
        }

        if ( target.exists() )
        {
            logger.info( "Using stored copy from artifact store: %s for: %s", store.getName(), path );
            return getStorageReference( store.getKey(), path );
        }
        else
        {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#download(org.commonjava.aprox.core.model.Repository,
     * java.lang.String, java.io.File, boolean)
     */
    private boolean download( final Repository repository, final StorageItem target, final boolean suppressFailures )
        throws AproxWorkflowException
    {
        final String url = buildDownloadUrl( repository, target.getPath(), suppressFailures );
        int timeoutSeconds = repository.getTimeoutSeconds();
        if ( timeoutSeconds < 1 )
        {
            timeoutSeconds = Repository.DEFAULT_TIMEOUT_SECONDS;
        }

        if ( !joinDownload( url, target, timeoutSeconds, suppressFailures ) )
        {
            startDownload( url, repository, target, timeoutSeconds, suppressFailures );
        }

        return target.exists();
    }

    private boolean startDownload( final String url, final Repository repository, final StorageItem target,
                                   final int timeoutSeconds, final boolean suppressFailures )
        throws AproxWorkflowException
    {
        final Downloader dl = new Downloader( url, repository, target, credProvider, client, fileEvent );

        final Future<StorageItem> future = executor.submit( dl );
        pending.put( url, future );

        boolean result = true;
        try
        {
            final StorageItem downloaded = future.get( timeoutSeconds, TimeUnit.SECONDS );

            if ( !suppressFailures && dl.getError() != null )
            {
                throw dl.getError();
            }

            result = downloaded != null && downloaded.exists();
        }
        catch ( final InterruptedException e )
        {
            if ( !suppressFailures )
            {
                throw new AproxWorkflowException( Response.status( Status.NO_CONTENT )
                                                          .build() );
            }
            result = false;
        }
        catch ( final ExecutionException e )
        {
            if ( !suppressFailures )
            {
                throw new AproxWorkflowException( Response.serverError()
                                                          .build() );
            }
            result = false;
        }
        catch ( final TimeoutException e )
        {
            if ( !suppressFailures )
            {
                throw new AproxWorkflowException( Response.status( Status.NO_CONTENT )
                                                          .build() );
            }
            result = false;
        }
        finally
        {
            synchronized ( pending )
            {
                logger.info( "Marking download complete: %s", url );
                pending.remove( url );
            }
        }

        return result;
    }

    private String buildDownloadUrl( final Repository repository, final String path, final boolean suppressFailures )
        throws AproxWorkflowException
    {
        final String remoteBase = repository.getUrl();
        String url = null;
        try
        {
            url = buildUrl( remoteBase, path );
        }
        catch ( final MalformedURLException e )
        {
            logger.error( "Invalid URL for path: %s in remote URL: %s. Reason: %s", e, path, remoteBase, e.getMessage() );

            if ( !suppressFailures )
            {
                throw new AproxWorkflowException( Response.status( Status.BAD_REQUEST )
                                                          .build() );
            }
            else
            {
                url = null;
            }
        }

        return url;
    }

    private boolean joinDownload( final String url, final StorageItem target, final int timeoutSeconds,
                                  final boolean suppressFailures )
        throws AproxWorkflowException
    {
        boolean result = target.exists();

        // if the target file already exists, skip joining.
        if ( !result )
        {
            synchronized ( pending )
            {
                // if we're already downloading, just wait for that to be done.
                if ( pending.containsKey( url ) )
                {
                    final Future<StorageItem> future = pending.get( url );
                    StorageItem f = null;
                    try
                    {
                        f = future.get( timeoutSeconds, TimeUnit.SECONDS );

                        // if the download landed in a different repository, copy it to the current one for
                        // completeness...

                        // NOTE: It'd be nice to alias instead of copying, but
                        // that would require a common centralized store
                        // to prevent removal of a repository from hosing
                        // the links.
                        if ( f != null && f.exists() && !f.equals( target ) )
                        {
                            target.copyFrom( f );
                        }

                        result = target != null && target.exists();
                    }
                    catch ( final InterruptedException e )
                    {
                        if ( !suppressFailures )
                        {
                            throw new AproxWorkflowException( Response.status( Status.NO_CONTENT )
                                                                      .build() );
                        }
                    }
                    catch ( final ExecutionException e )
                    {
                        if ( !suppressFailures )
                        {
                            throw new AproxWorkflowException( Response.serverError()
                                                                      .build() );
                        }
                    }
                    catch ( final TimeoutException e )
                    {
                        if ( !suppressFailures )
                        {
                            throw new AproxWorkflowException( Response.status( Status.NO_CONTENT )
                                                                      .build() );
                        }
                    }
                    catch ( final IOException e )
                    {
                        logger.error( "Failed to copy downloaded file to repository target. Error:  %s\nDownloaded location: %s\nRepository target: %s",
                                      e, e.getMessage(), f, target );

                        if ( !suppressFailures )
                        {
                            throw new AproxWorkflowException( Response.serverError()
                                                                      .build() );
                        }
                    }
                }
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#upload(org.commonjava.aprox.core.model.DeployPoint,
     * java.lang.String, java.io.InputStream)
     */
    @Override
    public StorageItem store( final DeployPoint deploy, final String path, final InputStream stream )
        throws AproxWorkflowException
    {
        final ArtifactPathInfo pathInfo = parsePathInfo( path );
        if ( pathInfo.isSnapshot() )
        {
            if ( !deploy.isAllowSnapshots() )
            {
                logger.error( "Cannot store snapshot in non-snapshot deploy point: %s", deploy.getName() );
                throw new AproxWorkflowException( Response.status( Status.BAD_REQUEST )
                                                          .build() );
            }
        }
        else if ( !deploy.isAllowReleases() )
        {
            logger.error( "Cannot store release in snapshot-only deploy point: %s", deploy.getName() );
            throw new AproxWorkflowException( Response.status( Status.BAD_REQUEST )
                                                      .build() );
        }

        final StorageItem target = getStorageReference( deploy, path );

        // TODO: Need some protection for released files!
        // if ( target.exists() )
        // {
        // throw new WebApplicationException(
        // Response.status( Status.BAD_REQUEST ).entity( "Deployment path already exists." ).build() );
        // }

        OutputStream out = null;
        try
        {
            out = target.openOutputStream();
            copy( stream, out );

            if ( fileEvent != null )
            {
                fileEvent.fire( new FileStorageEvent( FileStorageEvent.Type.UPLOAD, deploy, target ) );
            }
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to store: %s in deploy store: %s. Reason: %s", e, path, deploy.getName(),
                          e.getMessage() );

            throw new AproxWorkflowException( Response.serverError()
                                                      .build() );
        }
        finally
        {
            closeQuietly( out );
        }

        return target;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#upload(java.util.List, java.lang.String,
     * java.io.InputStream)
     */
    @Override
    public StorageItem store( final List<? extends ArtifactStore> stores, final String path, final InputStream stream )
        throws AproxWorkflowException
    {
        final ArtifactPathInfo pathInfo = parsePathInfo( path );

        DeployPoint selected = null;
        for ( final ArtifactStore store : stores )
        {
            if ( store instanceof DeployPoint )
            {
                final DeployPoint dp = (DeployPoint) store;
                if ( pathInfo.isSnapshot() )
                {
                    if ( dp.isAllowSnapshots() )
                    {
                        selected = dp;
                        break;
                    }
                }
                else if ( dp.isAllowReleases() )
                {
                    selected = dp;
                    break;
                }
            }
        }

        if ( selected == null )
        {
            logger.warn( "Cannot deploy. No valid deploy points in group." );
            throw new AproxWorkflowException( Response.status( Status.BAD_REQUEST )
                                                      .entity( "No deployment locations available." )
                                                      .build() );
        }

        store( selected, path, stream );

        return getStorageReference( selected.getKey(), path );
    }

    @Override
    public ArtifactPathInfo parsePathInfo( final String path )
    {
        if ( isEmpty( path ) || path.endsWith( "/" ) )
        {
            return null;
        }

        final String[] parts = path.split( "/" );
        if ( parts.length < 4 )
        {
            return null;
        }

        final String file = parts[parts.length - 1];
        final String version = parts[parts.length - 2];
        final String artifactId = parts[parts.length - 3];
        final StringBuilder groupId = new StringBuilder();
        for ( int i = 0; i < parts.length - 3; i++ )
        {
            if ( groupId.length() > 0 )
            {
                groupId.append( '.' );
            }

            groupId.append( parts[i] );
        }

        return new ArtifactPathInfo( groupId.toString(), artifactId, version, file, path );
    }

    @Override
    public StorageItem getStoreRootDirectory( final StoreKey key )
    {
        return new StorageItem( key, storage, StorageItem.ROOT );
    }

    @Override
    public StorageItem getStorageReference( final ArtifactStore store, final String... path )
    {
        return new StorageItem( store.getKey(), storage, path );
    }

    @Override
    public StorageItem getStorageReference( final StoreKey key, final String... path )
    {
        return new StorageItem( key, storage, path );
    }

    private static final class Downloader
        implements Callable<StorageItem>
    {

        private final Logger logger = new Logger( getClass() );

        private final String url;

        private final Repository repository;

        private final StorageItem target;

        private final TLRepositoryCredentialsProvider credProvider;

        private final Event<FileStorageEvent> fileEvent;

        private final HttpClient client;

        private AproxWorkflowException error;

        public Downloader( final String url, final Repository repository, final StorageItem target,
                           final TLRepositoryCredentialsProvider credProvider, final HttpClient client,
                           final Event<FileStorageEvent> fileEvent )
        {
            this.url = url;
            this.repository = repository;
            this.target = target;
            this.credProvider = credProvider;
            this.client = client;
            this.fileEvent = fileEvent;
        }

        @Override
        public StorageItem call()
        {
            credProvider.bind( repository );

            logger.info( "Trying: %s", url );

            final HttpGet request = new HttpGet( url );

            if ( repository.getProxyHost() != null )
            {
                final int proxyPort = repository.getProxyPort();
                HttpHost proxy;
                if ( proxyPort < 1 )
                {
                    proxy = new HttpHost( repository.getProxyHost() );
                }
                else
                {
                    proxy = new HttpHost( repository.getProxyHost(), repository.getProxyPort() );
                }

                request.getParams()
                       .setParameter( ConnRoutePNames.DEFAULT_PROXY, proxy );
            }

            request.getParams()
                   .setParameter( FileManager.HTTP_PARAM_REPO, repository );

            try
            {
                final InputStream in = executeGet( request, url );
                writeTarget( target, in, url, repository );
            }
            catch ( final AproxWorkflowException e )
            {
                this.error = e;
            }
            finally
            {
                cleanup( request );
            }

            return target;
        }

        public AproxWorkflowException getError()
        {
            return error;
        }

        private void writeTarget( final StorageItem target, final InputStream in, final String url,
                                  final Repository repository )
            throws AproxWorkflowException
        {
            OutputStream out = null;
            if ( in != null )
            {
                try
                {
                    out = target.openOutputStream();

                    copy( in, out );

                    if ( fileEvent != null )
                    {
                        fileEvent.fire( new FileStorageEvent( FileStorageEvent.Type.DOWNLOAD, repository, target ) );
                    }
                }
                catch ( final IOException e )
                {
                    logger.error( "Failed to write to local proxy store: %s\nOriginal URL: %s. Reason: %s", e, target,
                                  url, e.getMessage() );

                    throw new AproxWorkflowException( Response.serverError()
                                                              .build() );
                }
                finally
                {
                    closeQuietly( in );
                    closeQuietly( out );
                }
            }
        }

        private InputStream executeGet( final HttpGet request, final String url )
            throws AproxWorkflowException
        {
            InputStream result = null;

            try
            {
                final HttpResponse response = client.execute( request );
                final StatusLine line = response.getStatusLine();
                final int sc = line.getStatusCode();
                if ( sc != HttpStatus.SC_OK )
                {
                    logger.warn( "%s : %s", line, url );
                    if ( sc == HttpStatus.SC_NOT_FOUND )
                    {
                        result = null;
                    }
                    else
                    {
                        throw new AproxWorkflowException( Response.serverError()
                                                                  .build() );
                    }
                }
                else
                {
                    result = response.getEntity()
                                     .getContent();
                }
            }
            catch ( final ClientProtocolException e )
            {
                logger.warn( "Repository remote request failed for: %s. Reason: %s", e, url, e.getMessage() );
                throw new AproxWorkflowException( Response.serverError()
                                                          .build() );
            }
            catch ( final IOException e )
            {
                logger.warn( "Repository remote request failed for: %s. Reason: %s", e, url, e.getMessage() );
                throw new AproxWorkflowException( Response.serverError()
                                                          .build() );
            }

            return result;
        }

        private void cleanup( final HttpGet request )
        {
            credProvider.clear();
            request.abort();
            client.getConnectionManager()
                  .closeExpiredConnections();
            client.getConnectionManager()
                  .closeIdleConnections( 2, TimeUnit.SECONDS );
        }

    }

}
