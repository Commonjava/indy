/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.web.maven.proxy.rest.util;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
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
import javax.ws.rs.WebApplicationException;
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
import org.commonjava.util.logging.Logger;
import org.commonjava.web.maven.proxy.change.event.FileStorageEvent;
import org.commonjava.web.maven.proxy.conf.ProxyConfiguration;
import org.commonjava.web.maven.proxy.model.ArtifactStore;
import org.commonjava.web.maven.proxy.model.DeployPoint;
import org.commonjava.web.maven.proxy.model.Repository;

@Singleton
public class FileManager
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ProxyConfiguration config;

    @Inject
    private Event<FileStorageEvent> fileEvent;

    private HttpClient client;

    private final Set<String> pendingUrls = new HashSet<String>();

    private TLRepositoryCredentialsProvider credProvider;

    public FileManager()
    {}

    public FileManager( final ProxyConfiguration config )
    {
        this.config = config;
        setup();
    }

    @PostConstruct
    protected void setup()
    {
        ThreadSafeClientConnManager ccm = new ThreadSafeClientConnManager();
        ccm.setMaxTotal( 20 );

        credProvider = new TLRepositoryCredentialsProvider();

        DefaultHttpClient hc = new DefaultHttpClient( ccm );
        hc.setCredentialsProvider( credProvider );

        client = hc;
    }

    public File downloadFirst( final List<ArtifactStore> stores, final String path )
    {
        File result = null;
        File target = null;

        for ( ArtifactStore store : stores )
        {
            File dir = new File( config.getRepositoryRootDirectory(), store.getName() );
            target = new File( dir, path );

            if ( !target.exists() )
            {
                if ( ( store instanceof Repository )
                    && download( (Repository) store, path, target, true ) )
                {
                    result = target;
                    break;
                }
            }
            else
            {
                logger.info( "Using stored copy from artifact store: %s for: %s", store.getName(),
                             path );
                result = target;
                break;
            }
        }

        return result;
    }

    public Set<File> downloadAll( final List<ArtifactStore> stores, final String path )
    {
        Set<File> targets = new LinkedHashSet<File>();

        File target = null;
        for ( ArtifactStore store : stores )
        {
            File dir = new File( config.getRepositoryRootDirectory(), store.getName() );
            target = new File( dir, path );

            if ( !target.exists() )
            {
                if ( ( store instanceof Repository )
                    && download( (Repository) store, path, target, true ) )
                {
                    targets.add( target );
                }
            }
            else
            {
                logger.info( "Using stored copy from artifact store: %s for: %s", store.getName(),
                             path );
                targets.add( target );
                break;
            }
        }

        return targets;
    }

    public File download( final ArtifactStore store, final String path )
    {
        File dir = new File( config.getRepositoryRootDirectory(), store.getName() );
        File target = new File( dir, path );

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

    public boolean download( final Repository repository, final String path, final File target,
                             final boolean suppressFailures )
    {
        String url = buildDownloadUrl( repository, path, suppressFailures );

        if ( !continueDownload( url, repository.getTimeoutSeconds(), suppressFailures ) )
        {
            return target.exists();
        }

        credProvider.bind( repository );

        logger.info( "Trying: %s", url );

        HttpGet request = new HttpGet( url );
        try
        {
            InputStream in = executeGet( request, url, suppressFailures );
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

    private void writeTarget( final File target, final InputStream in, final String url,
                              final Repository repository, final String path,
                              final boolean suppressFailures )
    {
        FileOutputStream out = null;
        if ( in != null )
        {
            try
            {
                File targetDir = target.getParentFile();
                if ( !targetDir.exists() && !targetDir.mkdirs() )
                {
                    logger.error( "Cannot create repository local storage directory: %s", targetDir );
                    throw new WebApplicationException(
                                                       Response.status( Status.INTERNAL_SERVER_ERROR ).build() );
                }

                out = new FileOutputStream( target );

                copy( in, out );

                if ( fileEvent != null )
                {
                    fileEvent.fire( new FileStorageEvent( FileStorageEvent.Type.DOWNLOAD,
                                                          repository, path, target ) );
                }
            }
            catch ( IOException e )
            {
                logger.error( "Failed to write to local proxy store: %s\nOriginal URL: %s. Reason: %s",
                              e, target, url, e.getMessage() );

                if ( !suppressFailures )
                {
                    throw new WebApplicationException(
                                                       Response.status( Status.INTERNAL_SERVER_ERROR ).build() );
                }
            }
            finally
            {
                closeQuietly( in );
                closeQuietly( out );
            }
        }
    }

    private InputStream executeGet( final HttpGet request, final String url,
                                    final boolean suppressFailures )
    {
        InputStream result = null;

        try
        {
            HttpResponse response = client.execute( request );
            StatusLine line = response.getStatusLine();
            if ( line.getStatusCode() != HttpStatus.SC_OK )
            {
                logger.warn( "%s : %s", line, url );
                if ( !suppressFailures )
                {
                    throw new WebApplicationException(
                                                       Response.status( Status.INTERNAL_SERVER_ERROR ).build() );
                }
            }
            else
            {
                result = response.getEntity().getContent();
            }
        }
        catch ( ClientProtocolException e )
        {
            logger.warn( "Repository remote request failed for: %s. Reason: %s", e, url,
                         e.getMessage() );

            if ( !suppressFailures )
            {
                throw new WebApplicationException(
                                                   Response.status( Status.INTERNAL_SERVER_ERROR ).build() );
            }
            else
            {
                result = null;
            }
        }
        catch ( IOException e )
        {
            logger.warn( "Repository remote request failed for: %s. Reason: %s", e, url,
                         e.getMessage() );

            if ( !suppressFailures )
            {
                throw new WebApplicationException(
                                                   Response.status( Status.INTERNAL_SERVER_ERROR ).build() );
            }
            else
            {
                result = null;
            }
        }

        return result;
    }

    private String buildDownloadUrl( final Repository repository, final String path,
                                     final boolean suppressFailures )
    {
        String remoteBase = repository.getUrl();
        String url = null;
        try
        {
            url = buildUrl( remoteBase, path );
        }
        catch ( MalformedURLException e )
        {
            logger.error( "Invalid URL for path: %s in remote URL: %s. Reason: %s", e, path,
                          remoteBase, e.getMessage() );

            if ( !suppressFailures )
            {
                throw new WebApplicationException( Status.BAD_REQUEST );
            }
            else
            {
                url = null;
            }
        }

        return url;
    }

    private boolean continueDownload( final String url, final int timeoutSeconds,
                                      final boolean suppressFailures )
    {
        synchronized ( pendingUrls )
        {
            if ( pendingUrls.contains( url ) )
            {
                long timeout = System.currentTimeMillis() + ( timeoutSeconds * 1000 );
                do
                {
                    if ( System.currentTimeMillis() > timeout )
                    {
                        if ( !suppressFailures )
                        {
                            throw new WebApplicationException( Status.NO_CONTENT );
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
                    catch ( InterruptedException e )
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
        client.getConnectionManager().closeExpiredConnections();
        client.getConnectionManager().closeIdleConnections( 2, TimeUnit.SECONDS );
    }

    public void upload( final DeployPoint deploy, final String path, final InputStream stream )
    {
        File dir = new File( config.getRepositoryRootDirectory(), deploy.getName() );
        File target = new File( dir, path );

        // TODO: Need some protection for released files!
        // if ( target.exists() )
        // {
        // throw new WebApplicationException(
        // Response.status( Status.BAD_REQUEST ).entity( "Deployment path already exists." ).build() );
        // }

        File targetDir = target.getParentFile();
        targetDir.mkdirs();
        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream( target );
            copy( stream, out );

            if ( fileEvent != null )
            {
                fileEvent.fire( new FileStorageEvent( FileStorageEvent.Type.UPLOAD, deploy, path,
                                                      target ) );
            }
        }
        catch ( IOException e )
        {
            logger.error( "Failed to store: %s in deploy store: %s. Reason: %s", e, path,
                          deploy.getName(), e.getMessage() );

            throw new WebApplicationException(
                                               Response.status( Status.INTERNAL_SERVER_ERROR ).build() );
        }
        finally
        {
            closeQuietly( out );
        }
    }

    public DeployPoint upload( final List<DeployPoint> deployPoints, final String path,
                               final InputStream stream )
    {
        // TODO: Need to match the upload snapshot status to an appropriate deploy point...
        if ( deployPoints.isEmpty() )
        {
            throw new WebApplicationException(
                                               Response.status( Status.BAD_REQUEST ).entity( "No deployment locations available." ).build() );
        }

        DeployPoint deploy = deployPoints.get( 0 );
        upload( deploy, path, stream );

        return deploy;
    }

}
