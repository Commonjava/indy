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
package org.commonjava.aprox.core.ctl;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.core.expire.ScheduleManager;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.util.ApplicationStatus;

@ApplicationScoped
public class AdminController
{
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
        throws AproxWorkflowException
    {
        try
        {
            String changelog = store.getMetadata( ArtifactStore.METADATA_CHANGELOG );
            if ( changelog == null )
            {
                changelog = "Changelog not provided";
            }

            final ChangeSummary summary = new ChangeSummary( user, changelog );

            return storeManager.storeArtifactStore( store, summary, skipExisting );
        }
        catch ( final AproxDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to store: {}. Reason: {}", e, store.getKey(), e.getMessage() );
        }
    }

    public List<? extends ArtifactStore> getAllOfType( final StoreType type )
        throws AproxWorkflowException
    {
        try
        {
            return storeManager.getAllArtifactStores( type );
        }
        catch ( final AproxDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to list: {}. Reason: {}", e, type, e.getMessage() );
        }
    }

    public ArtifactStore get( final StoreKey key )
        throws AproxWorkflowException
    {
        try
        {
            return storeManager.getArtifactStore( key );
        }
        catch ( final AproxDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to retrieve: {}. Reason: {}", e, key, e.getMessage() );
        }
    }

    public void delete( final StoreKey key, final String user, final String changelog )
        throws AproxWorkflowException
    {
        try
        {
            storeManager.deleteArtifactStore( key, new ChangeSummary( user, changelog ) );
        }
        catch ( final AproxDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to delete: {}. Reason: {}", e, key, e.getMessage() );
        }
    }

}
