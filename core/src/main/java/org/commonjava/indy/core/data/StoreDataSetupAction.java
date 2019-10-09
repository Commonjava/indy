/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;

@Named( "Store-Initialization" )
public class StoreDataSetupAction
    implements StartupAction
{
    private static final String DEFAULT_SETUP = "default-setup";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager storeManager;

    @Override
    public String getId()
    {
        return "Default artifact store initialization";
    }

    @Override
    public void start() throws IndyLifecycleException
    {
        final ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "Initializing default data." );

        try
        {
            logger.info( "Verfiying that Indy basic stores are installed..." );
            storeManager.install();

            if ( !storeManager.query()
                              .packageType( MAVEN_PKG_KEY )
                              .storeType( RemoteRepository.class )
                              .containsByName( "central" ) )
            {
                final RemoteRepository central =
                        new RemoteRepository( MAVEN_PKG_KEY, "central", "https://repo.maven.apache.org/maven2/" );
                central.setCacheTimeoutSeconds( 86400 );
                storeManager.storeArtifactStore( central, summary, true, true,
                                                 new EventMetadata().set( StoreDataManager.EVENT_ORIGIN,
                                                                          DEFAULT_SETUP ) );
            }

            if ( !storeManager.query()
                              .packageType( MAVEN_PKG_KEY )
                              .storeType( HostedRepository.class )
                              .containsByName( "local-deployments" ) )
            {
                final HostedRepository local = new HostedRepository( MAVEN_PKG_KEY, "local-deployments" );
                local.setAllowReleases( true );
                local.setAllowSnapshots( true );
                local.setSnapshotTimeoutSeconds( 86400 );

                storeManager.storeArtifactStore( local, summary, true, true,
                                                 new EventMetadata().set( StoreDataManager.EVENT_ORIGIN,
                                                                          DEFAULT_SETUP ) );
            }

            if ( !storeManager.query()
                              .packageType( MAVEN_PKG_KEY )
                              .storeType( Group.class )
                              .containsByName( "public" ) )
            {
                final Group pub = new Group( MAVEN_PKG_KEY, "public" );
                pub.addConstituent( new StoreKey( MAVEN_PKG_KEY, StoreType.remote, "central" ) );
                pub.addConstituent( new StoreKey( MAVEN_PKG_KEY, StoreType.hosted, "local-deployments" ) );

                storeManager.storeArtifactStore( pub, summary, true, true,
                                                 new EventMetadata().set( StoreDataManager.EVENT_ORIGIN,
                                                                          DEFAULT_SETUP ) );
            }
        }
        catch ( final IndyDataException e )
        {
            throw new RuntimeException( "Failed to boot indy components: " + e.getMessage(), e );
        }
    }

    @Override
    public int getStartupPriority()
    {
        return 0;
    }
}
