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
package org.commonjava.aprox.setback.data;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.action.AproxLifecycleException;
import org.commonjava.aprox.action.StartupAction;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.subsys.datafile.DataFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named( "set-back-initializer" )
public class SetBackSettingsInitializer
    implements StartupAction
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private SetBackSettingsManager settingsManager;

    protected SetBackSettingsInitializer()
    {
    }

    public SetBackSettingsInitializer( final StoreDataManager storeManager, final SetBackSettingsManager settingsManager )
    {
        this.storeManager = storeManager;
        this.settingsManager = settingsManager;
    }

    @Override
    public String getId()
    {
        return "Set-Back settings.xml initializer";
    }

    @Override
    public void start()
        throws AproxLifecycleException
    {
        try
        {
            final List<ArtifactStore> stores = storeManager.getAllArtifactStores();

            for ( final ArtifactStore store : stores )
            {
                if ( StoreType.hosted == store.getKey()
                                              .getType() )
                {
                    continue;
                }

                final DataFile settingsXml = settingsManager.getSetBackSettings( store.getKey() );
                if ( settingsXml == null || !settingsXml.exists() )
                {
                    try
                    {
                        settingsManager.generateStoreSettings( store );
                    }
                    catch ( final SetBackDataException e )
                    {
                        logger.error( "Failed to generate SetBack settings.xml for: " + store.getKey(), e );
                    }
                }
            }
        }
        catch ( final AproxDataException e )
        {
            throw new AproxLifecycleException(
                                               "Failed to retrieve full list of ArtifactStores available on the system",
                                               e );
        }
    }

    @Override
    public int getStartupPriority()
    {
        return 40;
    }

}
