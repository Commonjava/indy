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
package org.commonjava.aprox.setback.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.setback.conf.SetbackConfig;
import org.commonjava.aprox.setback.data.SetBackDataException;
import org.commonjava.aprox.setback.data.SetBackSettingsManager;
import org.commonjava.aprox.subsys.datafile.DataFile;
import org.commonjava.aprox.util.ApplicationStatus;

@ApplicationScoped
public class SetBackController
{
    @Inject
    private SetBackSettingsManager manager;

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private SetbackConfig config;

    protected SetBackController()
    {
    }

    public SetBackController( final SetBackSettingsManager manager, final StoreDataManager storeManager, SetbackConfig config )
    {
        this.manager = manager;
        this.storeManager = storeManager;
        this.config = config;
    }

    public DataFile getSetBackSettings( final StoreKey key )
        throws AproxWorkflowException
    {
        checkEnabled();
        return manager.getSetBackSettings( key );
    }

    public boolean deleteSetBackSettings( final StoreKey key )
        throws AproxWorkflowException
    {
        checkEnabled();
        try
        {
            final ArtifactStore store = storeManager.getArtifactStore( key );
            if ( store != null )
            {
                if ( manager.deleteStoreSettings( store ) )
                {
                    manager.generateStoreSettings( store );
                    return true;
                }
            }
        }
        catch ( final SetBackDataException e )
        {
            throw new AproxWorkflowException( "Failed to delete SetBack settings.xml for: %s. Reason: %s", e, key,
                                              e.getMessage() );
        }
        catch ( final AproxDataException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve ArtifactStore for: %s. Reason: %s", e, key,
                                              e.getMessage() );
        }

        return false;
    }

    private void checkEnabled()
            throws AproxWorkflowException
    {
        if ( !config.isEnabled() )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "SetBack is disabled." );
        }
    }

}
