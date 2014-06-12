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
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
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
    private AutoProxCatalog catalog;

    @Inject
    private AproxHttpProvider http;

    protected AutoProxDataManagerDecorator()
    {
    }

    public AutoProxDataManagerDecorator( final MemoryStoreDataManager dataManager, final AutoProxCatalog catalog,
                                         final AproxHttpProvider http )
    {
        this.dataManager = dataManager;
        this.catalog = catalog;
        this.http = http;
    }

    protected final StoreDataManager getDelegate()
    {
        return dataManager;
    }

    @Override
    public Group getGroup( final String name )
        throws ProxyDataException
    {
        logger.debug( "DECORATED (getGroup: {})", name );
        Group g = dataManager.getGroup( name );

        if ( !catalog.isEnabled() )
        {
            logger.debug( "AutoProx decorator disabled; returning: {}", g );
            return g;
        }

        logger.debug( "AutoProx decorator active" );
        if ( g == null )
        {
            logger.debug( "AutoProx: creating repository for: {}", name );
            g = catalog.createGroup( name );

            if ( g != null )
            {
                try
                {
                    final RemoteRepository validationRepo = catalog.createGroupValidationRemote( name );
                    if ( validationRepo != null
                        && !checkUrlValidity( validationRepo, catalog.getRemoteValidationUrl( name ) ) )
                    {
                        logger.warn( "Invalid repository URL: {}", validationRepo.getUrl() );
                        return null;
                    }
                }
                catch ( final MalformedURLException e )
                {
                    throw new ProxyDataException(
                                                  "[AUTOPROX] Failed to validate new group from factory matching: '%s'. Reason: %s",
                                                  e, name, e.getMessage() );
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

        return g;
    }

    private synchronized boolean checkUrlValidity( final RemoteRepository repo, String url )
        throws MalformedURLException
    {
        if ( url == null )
        {
            url = repo.getUrl();
        }

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
        if ( !catalog.isEnabled() )
        {
            logger.debug( "AutoProx decorator disabled; returning: {}", repo );
            return repo;
        }

        logger.debug( "AutoProx decorator active" );
        if ( repo == null )
        {
            logger.info( "AutoProx: creating repository for: {}", name );

            try
            {
                repo = catalog.createRemoteRepository( name );
                if ( repo != null )
                {
                    if ( !checkUrlValidity( repo, catalog.getRemoteValidationUrl( name ) ) )
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
        if ( !catalog.isEnabled() )
        {
            logger.debug( "AutoProx decorator disabled; returning: {}", repo );
            return repo;
        }

        logger.debug( "AutoProx decorator active" );
        if ( repo == null )
        {
            logger.info( "AutoProx: creating repository for: {}", name );

            repo = catalog.createHostedRepository( name );
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
