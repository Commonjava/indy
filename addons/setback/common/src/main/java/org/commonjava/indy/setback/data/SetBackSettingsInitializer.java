/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.setback.data;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.setback.conf.SetbackConfig;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;

@Named( "set-back-initializer" )
public class SetBackSettingsInitializer
    implements StartupAction
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private SetBackSettingsManager settingsManager;

    @Inject
    private SetbackConfig config;

    protected SetBackSettingsInitializer()
    {
    }

    public SetBackSettingsInitializer( final StoreDataManager storeManager, final SetBackSettingsManager settingsManager, SetbackConfig config )
    {
        this.storeManager = storeManager;
        this.settingsManager = settingsManager;
        this.config = config;
    }

    @Override
    public String getId()
    {
        return "Set-Back settings.xml initializer";
    }

    @Override
    public void start()
        throws IndyLifecycleException
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        try
        {
            final List<ArtifactStore> stores = storeManager.query().packageType( MAVEN_PKG_KEY ).getAll();

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
        catch ( final IndyDataException e )
        {
            throw new IndyLifecycleException(
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
