/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.autoprox.data;

import static org.commonjava.aprox.util.UrlUtils.buildUrl;

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
import org.apache.http.client.methods.HttpHead;
import org.commonjava.aprox.autoprox.conf.AutoProxConfig;
import org.commonjava.aprox.autoprox.conf.AutoProxFactory;
import org.commonjava.aprox.autoprox.conf.FactoryMapping;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
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
        logger.debug( "DECORATED (getGroup: {})", name );
        Group g = dataManager.getGroup( name );

        if ( !config.isEnabled() )
        {
            logger.debug( "AutoProx decorator disabled; returning: {}", g );
            return g;
        }

        logger.debug( "AutoProx decorator active" );
        if ( g == null )
        {
            final AutoProxFactory factory = getFactory( name );
            if ( factory == null )
            {
                return null;
            }

            logger.debug( "AutoProx: creating repository for: {}", name );
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

                        for ( final StoreKey key : new ArrayList<StoreKey>( g.getConstituents() ) )
                        {
                            final ArtifactStore store = getArtifactStore( key );
                            if ( store == null )
                            {
                                g.removeConstituent( key );
                            }
                        }

                        dataManager.storeArtifactStore( g );
                    }
                }
            }
            catch ( final MalformedURLException e )
            {
                throw new ProxyDataException(
                                              "[AUTOPROX] Failed to create/validate new remote repository from factory matching: '%s'. Reason: %s",
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

    private synchronized boolean checkUrlValidity( final RemoteRepository repo, final String proxyUrl,
                                                   final String validationPath )
        throws MalformedURLException
    {
        final String url = validationPath == null ? proxyUrl : buildUrl( proxyUrl, validationPath );

        logger.debug( "\n\n\n\n\n[AutoProx] Checking URL: {}", url );
        final HttpHead head = new HttpHead( url );

        http.bindRepositoryCredentialsTo( repo, head );

        boolean result = false;
        try
        {
            final HttpResponse response = http.getClient()
                                              .execute( head );
            final StatusLine statusLine = response.getStatusLine();
            final int status = statusLine.getStatusCode();
            logger.debug( "[AutoProx] HTTP Status: {}", statusLine );
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
        logger.debug( "DECORATED (getRemoteRepository: {})", name );
        RemoteRepository repo = dataManager.getRemoteRepository( name );
        if ( !config.isEnabled() )
        {
            logger.debug( "AutoProx decorator disabled; returning: {}", repo );
            return repo;
        }

        logger.debug( "AutoProx decorator active" );
        if ( repo == null )
        {
            final AutoProxFactory factory = getFactory( name );
            if ( factory == null )
            {
                return null;
            }

            logger.info( "AutoProx: creating repository for: {}", name );

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
                throw new ProxyDataException(
                                              "[AUTOPROX] Failed to create/validate new remote repository from factory matching: '%s'. Reason: %s",
                                              e, name, e.getMessage() );
            }
        }

        return repo;
    }

    @Override
    public HostedRepository getHostedRepository( final String name )
        throws ProxyDataException
    {
        logger.debug( "DECORATED (getHostedRepository: {})", name );
        HostedRepository repo = dataManager.getHostedRepository( name );
        if ( !config.isEnabled() )
        {
            logger.debug( "AutoProx decorator disabled; returning: {}", repo );
            return repo;
        }

        logger.debug( "AutoProx decorator active" );
        if ( repo == null )
        {
            final AutoProxFactory factory = getFactory( name );
            if ( factory == null )
            {
                return null;
            }

            logger.info( "AutoProx: creating repository for: {}", name );

            repo = factory.createHostedRepository( name );
            if ( repo != null )
            {
                dataManager.storeHostedRepository( repo );
            }
        }

        return repo;
    }

    @Override
    public ArtifactStore getArtifactStore( final StoreKey key )
        throws ProxyDataException
    {
        if ( key.getType() == StoreType.group )
        {
            return getGroup( key.getName() );
        }
        else if ( key.getType() == StoreType.remote )
        {
            return getRemoteRepository( key.getName() );
        }
        else
        {
            return getHostedRepository( key.getName() );
        }
    }

    @Override
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
        throws ProxyDataException
    {
        getGroup( groupName );
        return dataManager.getOrderedConcreteStoresInGroup( groupName );
    }

    @Override
    public List<ArtifactStore> getOrderedStoresInGroup( final String groupName )
        throws ProxyDataException
    {
        getGroup( groupName );
        return dataManager.getOrderedStoresInGroup( groupName );
    }

    @Override
    public boolean hasRemoteRepository( final String name )
    {
        try
        {
            return getRemoteRepository( name ) != null;
        }
        catch ( final ProxyDataException e )
        {
            logger.error( String.format( "Failed to retrieve/create remote: %s. Reason: %s", name, e.getMessage() ), e );
        }

        return false;
    }

    @Override
    public boolean hasHostedRepository( final String name )
    {
        try
        {
            return getHostedRepository( name ) != null;
        }
        catch ( final ProxyDataException e )
        {
            logger.error( String.format( "Failed to retrieve/create hosted: %s. Reason: %s", name, e.getMessage() ), e );
        }

        return false;
    }

    @Override
    public boolean hasGroup( final String name )
    {
        try
        {
            return getGroup( name ) != null;
        }
        catch ( final ProxyDataException e )
        {
            logger.error( String.format( "Failed to retrieve/create group: %s. Reason: %s", name, e.getMessage() ), e );
        }

        return false;
    }

    @Override
    public boolean hasArtifactStore( final StoreKey key )
    {
        try
        {
            return getArtifactStore( key ) != null;
        }
        catch ( final ProxyDataException e )
        {
            logger.error( String.format( "Failed to retrieve/create: %s. Reason: %s", key, e.getMessage() ), e );
        }

        return false;
    }

}
