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
package org.commonjava.indy.dotmaven.data;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.dotmaven.DotMavenException;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreType;

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
                    constituents = dataManager.query()
                                              .packageType( MAVEN_PKG_KEY )
                                              .enabledState( true )
                                              .getOrderedConcreteStoresInGroup( store.getName() );
                }
                catch ( final IndyDataException e )
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
