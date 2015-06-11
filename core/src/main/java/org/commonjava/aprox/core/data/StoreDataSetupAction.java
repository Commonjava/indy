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
package org.commonjava.aprox.core.data;

import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.action.AproxLifecycleException;
import org.commonjava.aprox.action.MigrationAction;
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named( "Store-Initialization" )
public class StoreDataSetupAction
    implements MigrationAction
{
    public static final String DEFAULT_SETUP = "default-setup";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager storeManager;

    @Override
    public String getId()
    {
        return "Default artifact store initilialization";
    }

    @Override
    public int getMigrationPriority()
    {
        return 95;
    }

    @Override
    public boolean migrate()
        throws AproxLifecycleException
    {
        final ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "Initializing default data." );

        boolean changed = false;
        try
        {
            logger.info( "Verfiying that AProx basic stores are installed..." );
            storeManager.install();

            if ( !storeManager.hasRemoteRepository( "central" ) )
            {
                final RemoteRepository central =
                    new RemoteRepository( "central", "http://repo.maven.apache.org/maven2/" );
                central.setCacheTimeoutSeconds( 86400 );
                storeManager.storeArtifactStore( central, summary, true, true,
                                                 new EventMetadata().set( StoreDataManager.EVENT_ORIGIN, DEFAULT_SETUP ) );
                changed = true;
            }

            if ( !storeManager.hasHostedRepository( "local-deployments" ) )
            {
                final HostedRepository local = new HostedRepository( "local-deployments" );
                local.setAllowReleases( true );
                local.setAllowSnapshots( true );
                local.setSnapshotTimeoutSeconds( 86400 );

                storeManager.storeArtifactStore( local, summary, true, true,
                                                 new EventMetadata().set( StoreDataManager.EVENT_ORIGIN, DEFAULT_SETUP ) );
                changed = true;
            }

            if ( !storeManager.hasGroup( "public" ) )
            {
                final Group pub = new Group( "public" );
                pub.addConstituent( new StoreKey( StoreType.remote, "central" ) );
                pub.addConstituent( new StoreKey( StoreType.hosted, "local-deployments" ) );

                storeManager.storeArtifactStore( pub, summary, true, true,
                                                 new EventMetadata().set( StoreDataManager.EVENT_ORIGIN, DEFAULT_SETUP ) );
                changed = true;
            }
        }
        catch ( final AproxDataException e )
        {
            throw new RuntimeException( "Failed to boot aprox components: " + e.getMessage(), e );
        }

        return changed;
    }

}
