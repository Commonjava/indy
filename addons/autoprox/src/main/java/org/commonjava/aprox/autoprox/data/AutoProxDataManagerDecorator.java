/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.autoprox.data;

import static org.commonjava.aprox.util.UrlUtils.buildUrl;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpHead;
import org.commonjava.aprox.autoprox.conf.AutoProxConfig;
import org.commonjava.aprox.autoprox.conf.AutoProxFactory;
import org.commonjava.aprox.autoprox.conf.FactoryMapping;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.subsys.http.AproxHttpProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Decorator
public abstract class AutoProxDataManagerDecorator
    implements StoreDataManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Delegate
    @Any
    @Inject
    private StoreDataManager dataManager;

    @Inject
    private AutoProxConfig config;

    @Inject
    private AproxHttpProvider http;

    @Override
    public Group getGroup( final String name )
        throws ProxyDataException
    {
        //        logger.info( "DECORATED (getGroup: {})", name );
        Group g = dataManager.getGroup( name );

        if ( !config.isEnabled() )
        {
            //            logger.info( "AutoProx decorator disabled; returning: {}", g );
            return g;
        }

        //        logger.info( "AutoProx decorator active" );
        if ( g == null )
        {
            final AutoProxFactory factory = getFactory( name );
            if ( factory == null )
            {
                return null;
            }

            //            logger.info( "AutoProx: creating repository for: %s", name );
            try
            {
                final RemoteRepository remote = factory.createRemoteRepository( name );
                if ( remote == null || checkUrlValidity( remote, remote.getUrl(), factory.getRemoteValidationPath() ) )
                {
                    final HostedRepository hosted = factory.createHostedRepository( name );

                    g = factory.createGroup( name, remote, hosted );

                    if ( g != null )
                    {
                        if ( remote != null )
                        {
                            dataManager.storeArtifactStore( remote );
                        }

                        if ( hosted != null )
                        {
                            dataManager.storeArtifactStore( hosted );
                        }

                        dataManager.storeArtifactStore( g );
                    }
                }
            }
            catch ( final MalformedURLException e )
            {
                throw new ProxyDataException( "[AUTOPROX] Failed to create/validate new remote repository from factory matching: '%s'. Reason: %s",
                                              e, name, e.getMessage() );
            }
        }

        return g;
    }

    private AutoProxFactory getFactory( final String name )
    {
        for ( final FactoryMapping mapping : config.getFactoryMappings() )
        {
            if ( mapping.matchesName( name ) )
            {
                return mapping.getFactory();
            }
        }

        return null;
    }

    private synchronized boolean checkUrlValidity( final RemoteRepository repo, final String proxyUrl, final String validationPath )
        throws MalformedURLException
    {
        final String url = buildUrl( proxyUrl, validationPath );

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
            //            logger.info( "[AutoProx] HTTP Status: {}", statusLine );
            result = status == HttpStatus.SC_OK;
        }
        catch ( final ClientProtocolException e )
        {
            logger.warn( "[AutoProx] Cannot connect to target repository: '{}'.", url );
        }
        catch ( final IOException e )
        {
            logger.warn( "[AutoProx] Cannot connect to target repository: '{}'.", url );
        }
        finally
        {
            http.clearRepositoryCredentials();
            http.closeConnection();
        }

        return result;
    }

    @Override
    public RemoteRepository getRemoteRepository( final String name )
        throws ProxyDataException
    {
        //        logger.info( "DECORATED (getRepository: %s)", name );
        RemoteRepository repo = dataManager.getRemoteRepository( name );
        if ( !config.isEnabled() )
        {
            //            logger.info( "AutoProx decorator disabled; returning: {}", repo );
            return repo;
        }

        //        logger.info( "AutoProx decorator active" );
        if ( repo == null )
        {
            final AutoProxFactory factory = getFactory( name );
            if ( factory == null )
            {
                return null;
            }

            //            logger.info( "AutoProx: creating repository for: %s", name );

            try
            {
                repo = factory.createRemoteRepository( name );
                if ( repo != null )
                {
                    if ( !checkUrlValidity( repo, repo.getUrl(), factory.getRemoteValidationPath() ) )
                    {
                        logger.warn( "Invalid repository URL: {}", repo.getUrl() );
                        return null;
                    }

                    dataManager.storeRemoteRepository( repo );
                }
            }
            catch ( final MalformedURLException e )
            {
                throw new ProxyDataException( "[AUTOPROX] Failed to create/validate new remote repository from factory matching: '%s'. Reason: %s",
                                              e, name, e.getMessage() );
            }
        }

        return repo;
    }

}
