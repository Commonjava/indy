/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.koji.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.koji.conf.IndyKojiConfig;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.pkg.PackageTypeConstants;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class KojiRemovePemMigrationAction
                implements MigrationAction
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private IndyKojiConfig config;

    protected KojiRemovePemMigrationAction() { }

    public KojiRemovePemMigrationAction( final StoreDataManager storeDataManager, final IndyKojiConfig config )
    {
        this.storeDataManager = storeDataManager;
        this.config = config;
    }

    @Override
    public String getId()
    {
        return "Remove server PEM from Koji repos.";
    }

    @Override
    public boolean migrate() throws IndyLifecycleException
    {
        if ( config.getServerPemEnabled() )
        {
            logger.info( "Skip the migration if the server pem is needed. " );
            return true;
        }

        logger.info( "Starting to remove server PEM from Koji repo." );
        return doMigrate();
    }

    private boolean doMigrate() throws IndyLifecycleException
    {

        List<RemoteRepository> repos;
        try
        {
            repos = storeDataManager.query().packageType( PackageTypeConstants.PKG_TYPE_MAVEN ).getAllRemoteRepositories();
        }
        catch ( IndyDataException e )
        {
            throw new IndyLifecycleException( "Cannot retrieve all remote repositories. Reason: %s", e,
                                              e.getMessage() );
        }

        List<RemoteRepository> toStore = new ArrayList<>();
        repos.forEach( repo -> {
            if ( repo.getDescription() != null && repo.getDescription().contains( "Koji build" ) && repo.getServerCertPem() != null )
            {
                repo.setServerCertPem( null );
                toStore.add( repo );
            }
        } );

        final ChangeSummary changeSummary =
                        new ChangeSummary( ChangeSummary.SYSTEM_USER, "Remove the server PEM from Koji repo." );

        for ( RemoteRepository repo : toStore )
        {
            try
            {
                storeDataManager.storeArtifactStore( repo, changeSummary, false, false, new EventMetadata() );
            }
            catch ( IndyDataException e )
            {
                throw new IndyLifecycleException( "Failed to store %s. Reason: %s",
                                                  e, repo == null ? "NULL REPO" : repo.getKey(), e.getMessage() );
            }
        }

        logger.info( "Remove server PEM from Koji repo migration done. Result: {}", toStore.size() );
        if ( logger.isDebugEnabled() )
        {
            toStore.forEach( ( s ) -> logger.debug( s.getKey().toString() ) );
        }

        return !toStore.isEmpty();
    }

    @Override
    public int getMigrationPriority()
    {
        return 99;
    }

}
