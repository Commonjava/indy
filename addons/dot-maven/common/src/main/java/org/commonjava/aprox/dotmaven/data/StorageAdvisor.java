/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.dotmaven.data;

import static org.commonjava.aprox.model.StoreType.deploy_point;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.dotmaven.DotMavenException;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.StoreType;

@ApplicationScoped
public class StorageAdvisor
{

    //    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager dataManager;

    @SuppressWarnings( "incomplete-switch" )
    public StorageAdvice getStorageAdvice( final ArtifactStore store )
        throws DotMavenException
    {
        boolean deployable = false;
        boolean releases = true;
        boolean snapshots = false;
        DeployPoint deployableStore = null;

        final StoreType type = store.getKey()
                                    .getType();
        all: switch ( type )
        {
            case group:
            {
                List<ArtifactStore> constituents;
                try
                {
                    constituents = dataManager.getOrderedConcreteStoresInGroup( store.getName() );
                }
                catch ( final ProxyDataException e )
                {
                    throw new DotMavenException(
                                                 "Failed to retrieve constituent ArtifactStores for group: %s. Reason: %s",
                                                 e, store.getName(), e.getMessage() );
                }

                for ( final ArtifactStore as : constituents )
                {
                    if ( as.getKey()
                           .getType() == deploy_point )
                    {
                        deployable = true;

                        final DeployPoint dp = (DeployPoint) as;
                        deployableStore = dp;

                        // TODO: If we have two deploy points with different settings, only the first will be used here!
                        snapshots = dp.isAllowSnapshots();
                        releases = dp.isAllowReleases();

                        //                        logger.info( "\n\n\n\nDeploy point: %s allows releases? %s Releases boolean set to: %s\n\n\n\n",
                        //                                     dp.getName(), dp.isAllowReleases(), releases );

                        break all;
                    }
                }
                break;
            }
            case deploy_point:
            {
                deployable = true;

                final DeployPoint dp = (DeployPoint) store;
                deployableStore = dp;
                snapshots = dp.isAllowSnapshots();
                releases = dp.isAllowReleases();

                //                logger.info( "Deploy point: %s allows releases? %s", dp.getName(), dp.isAllowReleases() );
                break;
            }
        }

        return new StorageAdvice( store, deployableStore, deployable, releases, snapshots );
    }

}
