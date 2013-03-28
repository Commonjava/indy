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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.commonjava.aprox.change.event.ArtifactStoreRescanEvent;
import org.commonjava.aprox.change.event.FileAccessEvent;
import org.commonjava.aprox.change.event.FileErrorEvent;
import org.commonjava.aprox.change.event.FileEventManager;
import org.commonjava.aprox.change.event.FileNotFoundEvent;
import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.io.StorageProvider;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ArtifactPathInfo;
import org.commonjava.aprox.subsys.http.AproxHttp;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
public class DefaultFileManager
    implements FileManager
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private AproxConfiguration config;

    @Inject
    private StorageProvider storage;

    @Inject
    private Event<ArtifactStoreRescanEvent> rescanEvent;

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private FileEventManager fileEventManager;

    //    @Inject
    //    private NotFoundCache nfc;

    private final Map<String, Future<StorageItem>> pending = new ConcurrentHashMap<String, Future<StorageItem>>();

    // Byte, because it's small, and we really only care about the keys anyway.
    private final Map<StoreKey, Byte> rescansInProgress = new ConcurrentHashMap<StoreKey, Byte>();

    @Inject
    @ExecutorConfig( priority = 10, threads = 4, named = "file-manager" )
    private ExecutorService executor; // = Executors.newFixedThreadPool( 8 );

    @Inject
    private AproxHttp http;

    public DefaultFileManager()
    {
    }

    public DefaultFileManager( final AproxConfiguration config, final StorageProvider storage, final AproxHttp http/*,
                                                                                                                   final NotFoundCache nfc*/)
    {
        this.config = config;
        this.storage = storage;
        this.http = http;
        //        this.nfc = nfc;
        this.fileEventManager = new FileEventManager();
        executor = Executors.newFixedThreadPool( 10 );
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

            //            logger.info( "Attempting retrieval of: %s from store: %s", path, store );
            target = retrieve( store, path, true );
            if ( target != null )
            {
                //                logger.info( "Returning content from file: %s", target );
                return target;
            }
        }

        fileEventManager.fire( new FileNotFoundEvent( stores, path ) );
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

        if ( results.isEmpty() )
        {
            fileEventManager.fire( new FileNotFoundEvent( stores, path ) );
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
        try
        {
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
                //                logger.info( "Using stored copy from artifact store: %s for: %s", store.getName(), path );
                final StorageItem item = getStorageReference( store.getKey(), path );

                return item;
            }
            else
            {
                return null;
            }
        }
        catch ( final AproxWorkflowException e )
        {
            fileEventManager.fire( new FileErrorEvent( target, e ) );
            throw e;
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

        //        if ( nfc.hasEntry( url ) )
        //        {
        //            fileEventManager.fire( new FileNotFoundEvent( repository, target.getPath() ) );
        //            return false;
        //        }

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
        final Downloader dl = new Downloader( /**nfc,**/
        url, repository, target, http );

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
                                                          .build(), "Interrupted download: %s from: %s. Reason: %s", e,
                                                  url, repository, e.getMessage() );
            }
            result = false;
        }
        catch ( final ExecutionException e )
        {
            if ( !suppressFailures )
            {
                throw new AproxWorkflowException( Response.serverError()
                                                          .build(), "Failed to download: %s from: %s. Reason: %s", e,
                                                  url, repository, e.getMessage() );
            }
            result = false;
        }
        catch ( final TimeoutException e )
        {
            if ( !suppressFailures )
            {
                throw new AproxWorkflowException( Response.status( Status.NO_CONTENT )
                                                          .build(), "Timed-out download: %s from: %s. Reason: %s", e,
                                                  url, repository, e.getMessage() );
            }
            result = false;
        }
        finally
        {
            //            logger.info( "Marking download complete: %s", url );
            pending.remove( url );
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
            final Future<StorageItem> future = pending.get( url );
            if ( future != null )
            {
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
        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        if ( pathInfo != null && pathInfo.isSnapshot() )
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
            out = target.openOutputStream( false );
            copy( stream, out );
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
        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );

        DeployPoint selected = null;
        for ( final ArtifactStore store : stores )
        {
            if ( store instanceof DeployPoint )
            {
                //                logger.info( "Found deploy point: %s", store.getName() );
                final DeployPoint dp = (DeployPoint) store;
                if ( pathInfo == null )
                {
                    // probably not an artifact, most likely metadata instead...
                    //                    logger.info( "Selecting it for non-artifact storage: %s", path );
                    selected = dp;
                    break;
                }
                else if ( pathInfo.isSnapshot() )
                {
                    if ( dp.isAllowSnapshots() )
                    {
                        //                        logger.info( "Selecting it for snapshot storage: %s", pathInfo );
                        selected = dp;
                        break;
                    }
                }
                else if ( dp.isAllowReleases() )
                {
                    //                    logger.info( "Selecting it for release storage: %s", pathInfo );
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
        return new StorageItem( key, storage, fileEventManager, StorageItem.ROOT );
    }

    @Override
    public StorageItem getStorageReference( final ArtifactStore store, final String... path )
    {
        return new StorageItem( store.getKey(), storage, fileEventManager, path );
    }

    @Override
    public StorageItem getStorageReference( final StoreKey key, final String... path )
    {
        return new StorageItem( key, storage, fileEventManager, path );
    }

    private static final class Downloader
        implements Callable<StorageItem>
    {

        private final Logger logger = new Logger( getClass() );

        private final String url;

        private final Repository repository;

        private final StorageItem target;

        private final AproxHttp http;

        private AproxWorkflowException error;

        //        private final NotFoundCache nfc;

        public Downloader( /**final NotFoundCache nfc,**/
        final String url, final Repository repository, final StorageItem target, final AproxHttp client )
        {
            //            this.nfc = nfc;
            this.url = url;
            this.repository = repository;
            this.target = target;
            this.http = client;
        }

        @Override
        public StorageItem call()
        {
            //            logger.info( "Trying: %s", url );
            final HttpGet request = new HttpGet( url );

            http.bindRepositoryCredentialsTo( repository, request );

            try
            {
                final InputStream in = executeGet( request, url );
                writeTarget( target, in, url, repository );
            }
            catch ( final AproxWorkflowException e )
            {
                //                try
                //                {
                //                    nfc.addMissing( url );
                //                }
                //                catch ( final ProxyDataException e1 )
                //                {
                //                    logger.error( "Failed to add NFC entry for: %s. Reason: %s", e1, url, e1.getMessage() );
                //                }

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
                final HttpResponse response = http.getClient()
                                                  .execute( request );
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
            http.clearRepositoryCredentials();
            request.abort();
            http.closeConnection();
        }

    }

    @Override
    public boolean deleteAll( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        boolean result = false;
        for ( final ArtifactStore store : stores )
        {
            result = delete( store, path ) || result;
        }

        return result;
    }

    @Override
    public boolean delete( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        final StorageItem item = getStorageReference( store, path == null ? ROOT_PATH : path );
        return doDelete( item );
    }

    private Boolean doDelete( final StorageItem item )
        throws AproxWorkflowException
    {
        if ( !item.exists() )
        {
            return false;
        }

        if ( item.isDirectory() )
        {
            final String[] listing = item.list();
            for ( final String sub : listing )
            {
                if ( !doDelete( item.getChild( sub ) ) )
                {
                    return false;
                }
            }
        }
        else
        {
            try
            {
                if ( !item.delete() )
                {
                    throw new AproxWorkflowException( Response.serverError()
                                                              .build(), "Failed to delete: %s.", item );
                }
            }
            catch ( final IOException e )
            {
                throw new AproxWorkflowException( Response.serverError()
                                                          .build(), "Failed to delete stored location: %s. Reason: %s",
                                                  e, item, e.getMessage() );
            }
        }

        return true;
    }

    @Override
    public void rescanAll( final List<? extends ArtifactStore> stores )
        throws AproxWorkflowException
    {
        for ( final ArtifactStore store : stores )
        {
            rescan( store );
        }
    }

    @Override
    public void rescan( final ArtifactStore store )
        throws AproxWorkflowException
    {
        executor.execute( new Rescanner( getStorageReference( store.getKey() ), rescansInProgress, fileEventManager,
                                         rescanEvent ) );
    }

    private static final class Rescanner
        implements Runnable
    {
        private static final Byte IN_PROGRESS_FLAG = (byte) 0x1;

        private final Map<StoreKey, Byte> rescansInProgress;

        private final StorageItem start;

        private final Event<ArtifactStoreRescanEvent> rescanEvent;

        private final FileEventManager fileEventManager;

        public Rescanner( final StorageItem start, final Map<StoreKey, Byte> rescansInProgress,
                          final FileEventManager fileEventManager, final Event<ArtifactStoreRescanEvent> rescanEvent )
        {
            this.start = start;
            this.rescansInProgress = rescansInProgress;
            this.fileEventManager = fileEventManager;
            this.rescanEvent = rescanEvent;
        }

        @Override
        public void run()
        {
            final StoreKey storeKey = start.getStoreKey();
            synchronized ( rescansInProgress )
            {
                if ( rescansInProgress.containsKey( storeKey ) )
                {
                    return;
                }

                rescansInProgress.put( storeKey, IN_PROGRESS_FLAG );
            }

            try
            {
                if ( rescanEvent != null )
                {
                    rescanEvent.fire( new ArtifactStoreRescanEvent( start.getStoreKey() ) );
                }

                doRescan( start );
            }
            finally
            {
                synchronized ( rescansInProgress )
                {
                    rescansInProgress.remove( storeKey );
                }
            }
        }

        private void doRescan( final StorageItem item )
        {
            if ( !item.exists() )
            {
                return;
            }

            if ( item.isDirectory() )
            {
                final String[] listing = item.list();
                for ( final String sub : listing )
                {
                    doRescan( item.getChild( sub ) );
                }
            }

            fileEventManager.fire( new FileAccessEvent( item ) );
        }

    }

}
