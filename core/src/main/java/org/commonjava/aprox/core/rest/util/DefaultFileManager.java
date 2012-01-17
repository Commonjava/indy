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
package org.commonjava.aprox.core.rest.util;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.commonjava.couch.util.UrlUtils.buildUrl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.commonjava.aprox.core.change.event.FileStorageEvent;
import org.commonjava.aprox.core.conf.ProxyConfiguration;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.rest.RESTWorkflowException;
import org.commonjava.util.logging.Logger;

@Singleton
public class DefaultFileManager
    implements FileManager
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ProxyConfiguration config;

    @Inject
    private Event<FileStorageEvent> fileEvent;

    private HttpClient client;

    private final Set<String> pendingUrls = new HashSet<String>();

    private TLRepositoryCredentialsProvider credProvider;

    public DefaultFileManager()
    {
    }

    public DefaultFileManager( final ProxyConfiguration config )
    {
        this.config = config;
        setup();
    }

    @PostConstruct
    protected void setup()
    {
        final ThreadSafeClientConnManager ccm = new ThreadSafeClientConnManager();
        ccm.setMaxTotal( 20 );

        credProvider = new TLRepositoryCredentialsProvider();

        final DefaultHttpClient hc = new DefaultHttpClient( ccm );
        hc.setCredentialsProvider( credProvider );

        client = hc;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#downloadFirst(java.util.List, java.lang.String)
     */
    @Override
    public File downloadFirst( final List<? extends ArtifactStore> stores, final String path )
        throws RESTWorkflowException
    {
        File result = null;
        File target = null;

        for ( final ArtifactStore store : stores )
        {
            if ( store == null )
            {
                continue;
            }

            target = formatStorageReference( store, path );
            logger.info( "Checking for existence of: '%s' (in: %s)", target, store );

            if ( !target.exists() )
            {
                if ( ( store instanceof Repository ) && download( (Repository) store, path, target, true ) )
                {
                    result = target;
                    break;
                }
            }
            else
            {
                logger.info( "Using stored copy from artifact store: %s for: %s", store.getName(), path );
                result = target;
                break;
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#downloadAll(java.util.List, java.lang.String)
     */
    @Override
    public Set<File> downloadAll( final List<? extends ArtifactStore> stores, final String path )
        throws RESTWorkflowException
    {
        final Set<File> targets = new LinkedHashSet<File>();

        File target = null;
        for ( final ArtifactStore store : stores )
        {
            target = formatStorageReference( store, path );

            if ( !target.exists() )
            {
                if ( ( store instanceof Repository ) && download( (Repository) store, path, target, true ) )
                {
                    targets.add( target );
                }
            }
            else
            {
                logger.info( "Using stored copy from artifact store: %s for: %s", store.getName(), path );
                targets.add( target );
                break;
            }
        }

        return targets;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#download(org.commonjava.aprox.core.model.ArtifactStore,
     * java.lang.String)
     */
    @Override
    public File download( final ArtifactStore store, final String path )
        throws RESTWorkflowException
    {
        File target = formatStorageReference( store, path );

        if ( !target.exists() )
        {
            if ( store instanceof Repository )
            {
                download( (Repository) store, path, target, false );
            }
            else
            {
                target = null;
            }
        }
        else
        {
            logger.info( "Using stored copy from artifact store: %s for: %s", store.getName(), path );
        }

        return target;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#download(org.commonjava.aprox.core.model.Repository,
     * java.lang.String, java.io.File, boolean)
     */
    @Override
    public boolean download( final Repository repository, final String path, final File target,
                             final boolean suppressFailures )
        throws RESTWorkflowException
    {
        final String url = buildDownloadUrl( repository, path, suppressFailures );

        if ( !continueDownload( url, repository.getTimeoutSeconds(), suppressFailures ) )
        {
            return target.exists();
        }

        credProvider.bind( repository );

        logger.info( "Trying: %s", url );

        final HttpGet request = new HttpGet( url );
        try
        {
            final InputStream in = executeGet( request, url, suppressFailures );
            writeTarget( target, in, url, repository, path, suppressFailures );
        }
        finally
        {
            cleanup( request );
            synchronized ( pendingUrls )
            {
                pendingUrls.remove( url );
                pendingUrls.notifyAll();
            }
        }

        return target.exists();
    }

    private void writeTarget( final File target, final InputStream in, final String url, final Repository repository,
                              final String path, final boolean suppressFailures )
        throws RESTWorkflowException
    {
        FileOutputStream out = null;
        if ( in != null )
        {
            try
            {
                final File targetDir = target.getParentFile();
                if ( !targetDir.exists() && !targetDir.mkdirs() )
                {
                    logger.error( "Cannot create repository local storage directory: %s", targetDir );
                    throw new RESTWorkflowException( Response.serverError()
                                                             .build() );
                }

                out = new FileOutputStream( target );

                copy( in, out );

                if ( fileEvent != null )
                {
                    fileEvent.fire( new FileStorageEvent( FileStorageEvent.Type.DOWNLOAD, repository, path, target ) );
                }
            }
            catch ( final IOException e )
            {
                logger.error( "Failed to write to local proxy store: %s\nOriginal URL: %s. Reason: %s", e, target, url,
                              e.getMessage() );

                if ( !suppressFailures )
                {
                    throw new RESTWorkflowException( Response.serverError()
                                                             .build() );
                }
            }
            finally
            {
                closeQuietly( in );
                closeQuietly( out );
            }
        }
    }

    private InputStream executeGet( final HttpGet request, final String url, final boolean suppressFailures )
        throws RESTWorkflowException
    {
        InputStream result = null;

        try
        {
            final HttpResponse response = client.execute( request );
            final StatusLine line = response.getStatusLine();
            if ( line.getStatusCode() != HttpStatus.SC_OK )
            {
                logger.warn( "%s : %s", line, url );
                if ( !suppressFailures )
                {
                    throw new RESTWorkflowException( Response.serverError()
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

            if ( !suppressFailures )
            {
                throw new RESTWorkflowException( Response.serverError()
                                                         .build() );
            }
            else
            {
                result = null;
            }
        }
        catch ( final IOException e )
        {
            logger.warn( "Repository remote request failed for: %s. Reason: %s", e, url, e.getMessage() );

            if ( !suppressFailures )
            {
                throw new RESTWorkflowException( Response.serverError()
                                                         .build() );
            }
            else
            {
                result = null;
            }
        }

        return result;
    }

    private String buildDownloadUrl( final Repository repository, final String path, final boolean suppressFailures )
        throws RESTWorkflowException
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
                throw new RESTWorkflowException( Response.status( Status.BAD_REQUEST )
                                                         .build() );
            }
            else
            {
                url = null;
            }
        }

        return url;
    }

    private boolean continueDownload( final String url, final int timeoutSeconds, final boolean suppressFailures )
        throws RESTWorkflowException
    {
        synchronized ( pendingUrls )
        {
            if ( pendingUrls.contains( url ) )
            {
                final long timeout = System.currentTimeMillis() + ( timeoutSeconds * 1000 );
                do
                {
                    if ( System.currentTimeMillis() > timeout )
                    {
                        if ( !suppressFailures )
                        {
                            throw new RESTWorkflowException( Response.status( Status.NO_CONTENT )
                                                                     .build() );
                        }
                        else
                        {
                            break;
                        }
                    }

                    try
                    {
                        pendingUrls.wait( 1000 );
                    }
                    catch ( final InterruptedException e )
                    {
                        break;
                    }

                }
                while ( !pendingUrls.contains( url ) );

                return false;
            }
            else
            {
                pendingUrls.add( url );
            }
        }

        return true;
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

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#upload(org.commonjava.aprox.core.model.DeployPoint,
     * java.lang.String, java.io.InputStream)
     */
    @Override
    public void upload( final DeployPoint deploy, final String path, final InputStream stream )
        throws RESTWorkflowException
    {
        final ArtifactPathInfo pathInfo = parsePathInfo( path );
        if ( pathInfo.isSnapshot() )
        {
            if ( !deploy.isAllowSnapshots() )
            {
                logger.error( "Cannot store snapshot in non-snapshot deploy point: %s", deploy.getName() );
                throw new RESTWorkflowException( Response.status( Status.BAD_REQUEST )
                                                         .build() );
            }
        }
        else if ( !deploy.isAllowReleases() )
        {
            logger.error( "Cannot store release in snapshot-only deploy point: %s", deploy.getName() );
            throw new RESTWorkflowException( Response.status( Status.BAD_REQUEST )
                                                     .build() );
        }

        final File target = formatStorageReference( deploy, path );

        // TODO: Need some protection for released files!
        // if ( target.exists() )
        // {
        // throw new WebApplicationException(
        // Response.status( Status.BAD_REQUEST ).entity( "Deployment path already exists." ).build() );
        // }

        final File targetDir = target.getParentFile();
        targetDir.mkdirs();
        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream( target );
            copy( stream, out );

            if ( fileEvent != null )
            {
                fileEvent.fire( new FileStorageEvent( FileStorageEvent.Type.UPLOAD, deploy, path, target ) );
            }
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to store: %s in deploy store: %s. Reason: %s", e, path, deploy.getName(),
                          e.getMessage() );

            throw new RESTWorkflowException( Response.serverError()
                                                     .build() );
        }
        finally
        {
            closeQuietly( out );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#upload(java.util.List, java.lang.String,
     * java.io.InputStream)
     */
    @Override
    public DeployPoint upload( final List<? extends ArtifactStore> stores, final String path, final InputStream stream )
        throws RESTWorkflowException
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
            throw new RESTWorkflowException( Response.status( Status.BAD_REQUEST )
                                                     .entity( "No deployment locations available." )
                                                     .build() );
        }

        upload( selected, path, stream );

        return selected;
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

    /*
     * (non-Javadoc)
     * @see
     * org.commonjava.aprox.core.rest.util.FileManager#formatStorageReference(org.commonjava.aprox.core.model.ArtifactStore
     * , java.lang.String)
     */
    @Override
    public File formatStorageReference( final ArtifactStore store, final String path )
    {
        return new File( new File( config.getRepositoryRootDirectory(), store.getDoctype()
                                                                             .name() + "-" + store.getName() ), path );
    }

}
