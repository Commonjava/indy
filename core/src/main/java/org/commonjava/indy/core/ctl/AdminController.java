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
package org.commonjava.indy.core.ctl;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.core.expire.ScheduleManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AdminController
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager storeManager;

    /* Injected to make sure this gets initialized up front. */
    @SuppressWarnings( "unused" )
    @Inject
    private ScheduleManager scheduleManager;

    protected AdminController()
    {
    }

    public AdminController( final StoreDataManager storeManager, final ScheduleManager scheduleManager )
    {
        this.storeManager = storeManager;
        this.scheduleManager = scheduleManager;
    }

    public boolean store( final ArtifactStore store, final String user, final boolean skipExisting )
        throws IndyWorkflowException
    {
        try
        {
            String changelog = store.getMetadata( ArtifactStore.METADATA_CHANGELOG );
            if ( changelog == null )
            {
                changelog = "Changelog not provided";
            }

            final ChangeSummary summary = new ChangeSummary( user, changelog );

            logger.info( "Persisting artifact store: {} using: {}", store, storeManager );
            return storeManager.storeArtifactStore( store, summary, skipExisting, new EventMetadata() );
        }
        catch ( final IndyDataException e )
        {
            throw new IndyWorkflowException( ApplicationStatus.SERVER_ERROR.code(), "Failed to store: {}. Reason: {}",
                                              e, store.getKey(), e.getMessage() );
        }
    }

    public List<? extends ArtifactStore> getAllOfType( final StoreType type )
        throws IndyWorkflowException
    {
        try
        {
            return storeManager.getAllArtifactStores( type );
        }
        catch ( final IndyDataException e )
        {
            throw new IndyWorkflowException( ApplicationStatus.SERVER_ERROR.code(), "Failed to list: {}. Reason: {}",
                                              e, type, e.getMessage() );
        }
    }

    public ArtifactStore get( final StoreKey key )
        throws IndyWorkflowException
    {
        try
        {
            return storeManager.getArtifactStore( key );
        }
        catch ( final IndyDataException e )
        {
            throw new IndyWorkflowException( ApplicationStatus.SERVER_ERROR.code(),
                                              "Failed to retrieve: {}. Reason: {}", e, key, e.getMessage() );
        }
    }

    public void delete( final StoreKey key, final String user, final String changelog )
        throws IndyWorkflowException
    {
        try
        {
            storeManager.deleteArtifactStore( key, new ChangeSummary( user, changelog ), new EventMetadata() );
        }
        catch ( final IndyDataException e )
        {
            throw new IndyWorkflowException( ApplicationStatus.SERVER_ERROR.code(),
                                              "Failed to delete: {}. Reason: {}", e, key, e.getMessage() );
        }
    }

    public boolean exists( final StoreKey key )
    {
        return storeManager.hasArtifactStore( key );
    }

}
