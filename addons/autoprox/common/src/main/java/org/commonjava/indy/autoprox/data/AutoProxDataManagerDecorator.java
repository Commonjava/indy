/**
 * Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.autoprox.data;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.ArtifactStoreQuery;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import java.util.ArrayList;

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
    private AutoProxCatalogManager catalog;

    @Inject
    private TransferManager transferManager;

    protected AutoProxDataManagerDecorator()
    {
    }

    public AutoProxDataManagerDecorator( final MemoryStoreDataManager dataManager, final AutoProxCatalogManager catalog,
                                         final TransferManager transferManager )
    {
        this.dataManager = dataManager;
        this.catalog = catalog;
        this.transferManager = transferManager;
    }

    @Override
    public ArtifactStoreQuery<ArtifactStore> query()
    {
        logger.debug( "Rewrapping query for data manager: {}", this );
        return dataManager.query().rewrap( this );
    }

    protected final StoreDataManager getDelegate()
    {
        return dataManager;
    }

    private Group getGroup( final StoreKey key, final StoreKey impliedBy )
            throws IndyDataException
    {
        logger.debug( "DECORATED (getGroup: {})", key );
        Group g = (Group) dataManager.getArtifactStore( key );

        if ( !catalog.isEnabled() )
        {
            logger.debug( "AutoProx decorator disabled; returning: {}", g );
            return g;
        }

        logger.debug( "AutoProx decorator active" );
        if ( g == null )
        {
            logger.debug( "AutoProx: creating repository for: {}", key );
            if ( !checkValidity( key ) )
            {
                return null;
            }

            try
            {
                g = catalog.createGroup( key );
            }
            catch ( final AutoProxRuleException e )
            {
                throw new IndyDataException(
                        "[AUTOPROX] Failed to create new group from factory matching: '%s'. Reason: %s", e, key,
                        e.getMessage() );
            }

            if ( g != null )
            {
                logger.info( "Validating group: {}", g );
                for ( final StoreKey memberKey : new ArrayList<>( g.getConstituents() ) )
                {
                    final ArtifactStore store = getArtifactStore( memberKey, impliedBy == null ? g.getKey() : impliedBy );
                    if ( store == null )
                    {
                        g.removeConstituent( memberKey );
                    }
                }

                if ( g.getConstituents().isEmpty() )
                {
                    return null;
                }

                final EventMetadata eventMetadata =
                        new EventMetadata().set( StoreDataManager.EVENT_ORIGIN, AutoProxConstants.STORE_ORIGIN )
                                           .set( AutoProxConstants.ORIGINATING_STORE,
                                                 impliedBy == null ? g.getKey() : impliedBy );

                dataManager.storeArtifactStore( g, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                          "AUTOPROX: Creating group for: '" + key
                                                                                  + "'" ),
                                                false, true, eventMetadata );
            }
        }

        return g;
    }

    /**
     * Validates the remote connection, produced from rule-set for given key,
     * for a remote repo or group containing a remote. If:
     *
     * <ul>
     *   <li>rule.isValidationEnabled() == false, return true</li>
     *   <li>rule.getValidationRemote() == null, return true</li>
     *   <li>
     *     rule.getRemoteValidationPath() != null, validate remote.getUrl() + validationPath
     *     <ul>
     *       <li>if response code is 200 OK, then return true</li>
     *       <li>otherwise, return false</li>
     *     </ul>
     *   </li>
     * </ul>
     * @throws IndyDataException if the selected rule encounters an error while creating the new group/repository instance(s).
     */
    private boolean checkValidity( final StoreKey key )
            throws IndyDataException
    {
        if ( catalog.isValidationEnabled( key ) )
        {
            try
            {
                final RemoteRepository validationRepo = catalog.createValidationRemote( key );
                if ( validationRepo == null )
                {
                    logger.info( "No validation repository was created: assuming {} is valid.", key );
                    return true;
                }

                String path = catalog.getRemoteValidationPath( key );
                if ( path == null )
                {
                    path = PathUtils.ROOT;
                }

                logger.debug( "\n\n\n\n\n[AutoProx] Checking path: {} under remote URL: {}", path, validationRepo.getUrl() );
                boolean result = false;
                try
                {
                    result = transferManager.exists(
                            new ConcreteResource( LocationUtils.toLocation( validationRepo ), path ) );
                }
                catch ( final TransferException e )
                {
                    logger.warn( "[AutoProx] Cannot connect to target repository: '{}'. Reason: {}", validationRepo, e.getMessage() );
                    logger.debug( "[AutoProx] exception from validation attempt for: " + validationRepo, e );
                }

                logger.debug( "Validation result for: {} is: {}", validationRepo, result );

                return result;
            }
            catch ( final AutoProxRuleException e )
            {
                throw new IndyDataException(
                        "[AUTOPROX] Failed to create new group from factory matching: '%s'. Reason: %s", e, key,
                        e.getMessage() );
            }
        }

        return true;
    }

    private RemoteRepository getRemoteRepository( final StoreKey key, final StoreKey impliedBy )
            throws IndyDataException
    {
        logger.debug( "DECORATED (getRemoteRepository: {})", key );
        RemoteRepository repo = (RemoteRepository) dataManager.getArtifactStore( key );
        if ( !catalog.isEnabled() )
        {
            logger.debug( "AutoProx decorator disabled; returning: {}", repo );
            return repo;
        }

        logger.debug( "AutoProx decorator active" );
        if ( repo == null )
        {
            logger.info( "AutoProx: creating repository for: {}", key );

            try
            {
                repo = catalog.createRemoteRepository( key );
                if ( repo != null )
                {
                    if ( !checkValidity( key ) )
                    {
                        return null;
                    }

                    final EventMetadata eventMetadata =
                            new EventMetadata().set( StoreDataManager.EVENT_ORIGIN, AutoProxConstants.STORE_ORIGIN )
                                               .set( AutoProxConstants.ORIGINATING_STORE,
                                                     impliedBy == null ? repo.getKey() : impliedBy );

                    dataManager.storeArtifactStore( repo, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                               "AUTOPROX: Creating remote repository for: '"
                                                                                       + key + "'" ),
                                                    false, true, eventMetadata );
                }
            }
            catch ( final AutoProxRuleException e )
            {
                throw new IndyDataException(
                        "[AUTOPROX] Failed to create new remote repository from factory matching: '%s'. Reason: %s", e,
                        key, e.getMessage() );
            }
        }

        return repo;
    }

    private HostedRepository getHostedRepository( final StoreKey key, final StoreKey impliedBy )
            throws IndyDataException
    {
        HostedRepository repo = (HostedRepository) dataManager.getArtifactStore( key );
        if ( !catalog.isEnabled() )
        {
            logger.debug( "AutoProx decorator disabled; returning: {}", repo );
            return repo;
        }

        logger.debug( "AutoProx decorator active" );
        if ( repo == null )
        {
            logger.info( "AutoProx: creating repository for: {}", key );

            try
            {
                repo = catalog.createHostedRepository( key );
            }
            catch ( final AutoProxRuleException e )
            {
                throw new IndyDataException(
                        "[AUTOPROX] Failed to create new hosted repository from factory matching: '%s'. Reason: %s", e,
                        key, e.getMessage() );
            }

            if ( repo != null )
            {
                final ChangeSummary changeSummary = new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                       "AUTOPROX: Creating hosted repository for: '"
                                                                               + key + "'" );

                final EventMetadata eventMetadata =
                        new EventMetadata().set( StoreDataManager.EVENT_ORIGIN, AutoProxConstants.STORE_ORIGIN )
                                           .set( AutoProxConstants.ORIGINATING_STORE,
                                                 impliedBy == null ? repo.getKey() : impliedBy );

                dataManager.storeArtifactStore( repo, changeSummary, false, true,
                                                eventMetadata );
            }
        }

        return repo;
    }

    @Override
    public ArtifactStore getArtifactStore( final StoreKey key )
            throws IndyDataException
    {
        return getArtifactStore( key, null );
    }

    private ArtifactStore getArtifactStore( final StoreKey key, final StoreKey impliedBy )
            throws IndyDataException
    {
        if ( key == null )
        {
            return null;
        }

        logger.debug( "DECORATED (getArtifactStore: {})", key );
        if ( key.getType() == StoreType.group )
        {
            return getGroup( key, impliedBy );
        }
        else if ( key.getType() == StoreType.remote )
        {
            return getRemoteRepository( key, impliedBy );
        }
        else
        {
            return getHostedRepository( key, impliedBy );
        }
    }

    @Override
    public boolean hasArtifactStore( final StoreKey key )
    {
        try
        {
            return getArtifactStore( key ) != null;
        }
        catch ( final IndyDataException e )
        {
            logger.error( String.format( "Failed to retrieve/create: %s. Reason: %s", key, e.getMessage() ), e );
        }

        return false;
    }

}
