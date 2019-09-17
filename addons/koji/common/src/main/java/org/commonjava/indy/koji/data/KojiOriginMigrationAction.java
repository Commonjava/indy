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
package org.commonjava.indy.koji.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.commonjava.indy.koji.model.IndyKojiConstants.KOJI_ORIGIN;
import static org.commonjava.indy.model.core.ArtifactStore.METADATA_ORIGIN;

/**
 * Created by jdcasey on 9/19/16.
 */
public class KojiOriginMigrationAction
        implements MigrationAction
{
    @Inject
    private StoreDataManager storeDataManager;

    @Override
    public boolean migrate()
            throws IndyLifecycleException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Disabled." );
        return true;
    }

    private boolean doMigrate()
            throws IndyLifecycleException
    {
        List<RemoteRepository> repos;
        try
        {
            repos = storeDataManager.query().noPackageType().getAllRemoteRepositories();
        }
        catch ( IndyDataException e )
        {
            throw new IndyLifecycleException( "Cannot retrieve all remote repositories. Reason: %s", e,
                                              e.getMessage() );
        }

        List<RemoteRepository> toStore = new ArrayList<>();
        repos.forEach( (repo)->{
            if ( repo.getDescription() != null && repo.getDescription().contains( "Koji build" ) )
            {
                repo.setMetadata( METADATA_ORIGIN, KOJI_ORIGIN );
                toStore.add( repo );
            }
        } );

        final ChangeSummary changeSummary =
                new ChangeSummary( ChangeSummary.SYSTEM_USER, "Adding Koji origin metadata" );

        for ( RemoteRepository repo : toStore )
        {
            try
            {
                storeDataManager.storeArtifactStore( repo, changeSummary, false, true, new EventMetadata() );
            }
            catch ( IndyDataException e )
            {
                throw new IndyLifecycleException( "Failed to store %s with Koji origin metadata. Reason: %s",
                                                  e, repo == null ? "NULL REPO" : repo.getKey(), e.getMessage() );
            }
        }

        return !toStore.isEmpty();
    }

    @Override
    public int getMigrationPriority()
    {
        return 10;
    }

    @Override
    public String getId()
    {
        return "Koji origin metadata migration";
    }
}
