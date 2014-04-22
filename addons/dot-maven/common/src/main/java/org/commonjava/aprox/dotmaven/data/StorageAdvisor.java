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
package org.commonjava.aprox.dotmaven.data;

import static org.commonjava.aprox.model.StoreType.hosted;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.dotmaven.DotMavenException;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.StoreType;

@ApplicationScoped
public class StorageAdvisor
{

    //    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager dataManager;

    public StorageAdvice getStorageAdvice( final ArtifactStore store )
        throws DotMavenException
    {
        boolean deployable = false;
        boolean releases = true;
        boolean snapshots = false;
        HostedRepository hostedStore = null;

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
                    throw new DotMavenException( "Failed to retrieve constituent ArtifactStores for group: %s. Reason: %s", e, store.getName(),
                                                 e.getMessage() );
                }

                for ( final ArtifactStore as : constituents )
                {
                    if ( as.getKey()
                           .getType() == hosted )
                    {
                        deployable = true;

                        hostedStore = (HostedRepository) as;
                        snapshots = hostedStore.isAllowSnapshots();
                        releases = hostedStore.isAllowReleases();

                        break all;
                    }
                }
                break;
            }
            case hosted:
            {
                deployable = true;

                hostedStore = (HostedRepository) store;
                snapshots = hostedStore.isAllowSnapshots();
                releases = hostedStore.isAllowReleases();
                break;
            }
            default:
        }

        return new StorageAdvice( store, hostedStore, deployable, releases, snapshots );
    }

}
