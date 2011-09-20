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
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
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
import org.commonjava.web.maven.proxy.conf.ProxyConfiguration;
import org.commonjava.web.maven.proxy.model.Repository;

@Singleton
public class Downloader
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ProxyConfiguration config;

    private HttpClient client;

    private TLRepositoryCredentialsProvider credProvider;

    public Downloader()
    {}

    public Downloader( final ProxyConfiguration config )
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

    public File downloadFirst( final List<Repository> repos, final String path )
    {
        File target = null;

        for ( Repository repo : repos )
        {
            File dir = new File( config.getRepositoryRootDirectory(), repo.getName() );
            target = new File( dir, path );

            if ( !target.exists() )
            {
                if ( download( repo, path, target, true ) )
                {
                    break;
                }
            }
            else
            {
                logger.info( "Using stored copy from repository: %s for: %s", repo.getName(), path );
                break;
            }
        }

        return target;
    }

    public File download( final Repository repo, final String path )
    {
        File dir = new File( config.getRepositoryRootDirectory(), repo.getName() );
        File target = new File( dir, path );

        if ( !target.exists() )
        {
            download( repo, path, target, false );
        }
        else
        {
            logger.info( "Using stored copy from repository: %s for: %s", repo.getName(), path );
        }

        return target;
    }

    public boolean download( final Repository repository, final String path, final File target,
                             final boolean suppressFailures )
    {
        credProvider.bind( repository );

        boolean proceed = true;

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
                throw new WebApplicationException( Response.status( Status.BAD_REQUEST ).build() );
            }
            else
            {
                proceed = false;
            }
        }

        if ( !proceed )
        {
            return false;
        }

        logger.info( "Trying: %s", url );

        HttpGet request = new HttpGet( url );
        try
        {
            HttpResponse response = client.execute( request );
            StatusLine line = response.getStatusLine();
            if ( line.getStatusCode() != HttpStatus.SC_OK )
            {
                logger.error( "Repository remote request failed: %s\nResponse status: %s", url,
                              line );
                if ( !suppressFailures )
                {
                    throw new WebApplicationException(
                                                       Response.status( Status.INTERNAL_SERVER_ERROR ).build() );
                }
                else
                {
                    proceed = false;
                }
            }

            if ( !proceed )
            {
                return false;
            }

            FileOutputStream out = null;
            InputStream in = null;
            try
            {
                File targetDir = target.getParentFile();
                if ( !targetDir.exists() && !targetDir.mkdirs() )
                {
                    logger.error( "Cannot create repository local storage directory: %s", targetDir );
                    throw new WebApplicationException(
                                                       Response.status( Status.INTERNAL_SERVER_ERROR ).build() );
                }

                in = response.getEntity().getContent();
                out = new FileOutputStream( target );

                copy( in, out );
            }
            finally
            {
                closeQuietly( in );
                closeQuietly( out );
            }
        }
        catch ( ClientProtocolException e )
        {
            logger.error( "Repository remote request failed for: %s. Reason: %s", e, url,
                          e.getMessage() );
            if ( !suppressFailures )
            {
                throw new WebApplicationException(
                                                   Response.status( Status.INTERNAL_SERVER_ERROR ).build() );
            }
            else
            {
                proceed = false;
            }
        }
        catch ( IOException e )
        {
            logger.error( "Repository remote request failed for: %s. Reason: %s", e, url,
                          e.getMessage() );
            if ( !suppressFailures )
            {
                throw new WebApplicationException(
                                                   Response.status( Status.INTERNAL_SERVER_ERROR ).build() );
            }
            else
            {
                proceed = false;
            }
        }
        finally
        {
            cleanup( request );
        }

        return proceed;
    }

    private void cleanup( final HttpGet request )
    {
        credProvider.clear();
        request.abort();
        client.getConnectionManager().closeExpiredConnections();
        client.getConnectionManager().closeIdleConnections( 2, TimeUnit.SECONDS );
    }

}
