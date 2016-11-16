/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.implrepo.data;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.RemoteRepository;
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
import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Wrap methods that are storeXXX(..), make sure all the {@link RemoteRepository} returned are checked the validity.
 */
@Decorator
public abstract class ValidRemoteStoreDataManagerDecorator
        implements StoreDataManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Delegate
    @Inject
    private StoreDataManager delegate;

    @Inject
    private TransferManager transferManager;

    protected ValidRemoteStoreDataManagerDecorator()
    {
    }

    public ValidRemoteStoreDataManagerDecorator( final StoreDataManager delegate,
                                                 final TransferManager transferManager )
    {
        this.delegate = delegate;
        this.transferManager = transferManager;
    }

    protected final StoreDataManager getDelegate()
    {
        return delegate;
    }

    @Override
    public boolean storeArtifactStore( ArtifactStore key, ChangeSummary summary )
            throws IndyDataException
    {
        if ( key instanceof RemoteRepository && !checkValidity( key ) )
        {
            return false;
        }
        return delegate.storeArtifactStore( key, summary );
    }

    @Override
    public boolean storeArtifactStore( ArtifactStore key, ChangeSummary summary, EventMetadata eventMetadata )
            throws IndyDataException
    {
        if ( key instanceof RemoteRepository && !checkValidity( key ) )
        {
            return false;
        }
        return delegate.storeArtifactStore( key, summary, eventMetadata );
    }

    @Override
    public boolean storeArtifactStore( ArtifactStore key, ChangeSummary summary, boolean skipIfExists )
            throws IndyDataException
    {
        if ( key instanceof RemoteRepository && !checkValidity( key ) )
        {
            return false;
        }
        return delegate.storeArtifactStore( key, summary, skipIfExists );
    }

    @Override
    public boolean storeArtifactStore( ArtifactStore key, ChangeSummary summary, boolean skipIfExists,
                                       EventMetadata eventMetadata )
            throws IndyDataException
    {
        if ( key instanceof RemoteRepository && !checkValidity( key ) )
        {
            return false;
        }
        return delegate.storeArtifactStore( key, summary, skipIfExists, eventMetadata );
    }

    @Override
    public boolean storeArtifactStore( ArtifactStore key, ChangeSummary summary, boolean skipIfExists,
                                       boolean fireEvents )
            throws IndyDataException
    {
        if ( key instanceof RemoteRepository && !checkValidity( key ) )
        {
            return false;
        }
        return delegate.storeArtifactStore( key, summary, skipIfExists, fireEvents );
    }

    @Override
    public boolean storeArtifactStore( ArtifactStore key, ChangeSummary summary, boolean skipIfExists,
                                       boolean fireEvents, EventMetadata eventMetadata )
            throws IndyDataException
    {
        if ( key instanceof RemoteRepository && !checkValidity( key ) )
        {
            return false;
        }
        return delegate.storeArtifactStore( key, summary, skipIfExists, fireEvents, eventMetadata );
    }

    private boolean checkValidity( ArtifactStore key )
    {
        RemoteRepository remoteRepository = (RemoteRepository) key;
        URL url = null;
        try
        {
            url = new URL( remoteRepository.getUrl() );
            ConcreteResource concreteResource =
                    new ConcreteResource( LocationUtils.toLocation( remoteRepository ), PathUtils.ROOT );

            return transferManager.exists( concreteResource );
        }
        catch ( final MalformedURLException e )
        {
            logger.error( "[RemoteValidation] Failed to parse repository URL: '{}'. Reason: {}", e, url,
                          e.getMessage() );
        }
        catch ( final TransferException e )
        {
            logger.warn( "[RemoteValidation] Cannot connect to target repository: '{}'. Reason: {}", this,
                         e.getMessage() );
            logger.debug( "[RemoteValidation] exception from validation attempt for: {}", this, e );
        }

        return false;
    }
}
