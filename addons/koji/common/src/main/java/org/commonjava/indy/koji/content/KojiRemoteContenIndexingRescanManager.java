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
package org.commonjava.indy.koji.content;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.change.event.ArtifactStorePostRescanEvent;
import org.commonjava.indy.change.event.ArtifactStorePreRescanEvent;
import org.commonjava.indy.content.index.ContentIndexManager;
import org.commonjava.indy.content.index.ContentIndexRescanManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.commonjava.indy.koji.model.IndyKojiConstants.KOJI_ORIGIN;

/**
 * This component will handle the content index for the Koji generated remote repo during its rescan. It will rebuild all relevant
 * index entries for the remote repo, and remove all useless entries for the missed artifacts in hosted repo. All these entries will
 * be based on the remote path masks.
 */
@ApplicationScoped
public class KojiRemoteContenIndexingRescanManager implements ContentIndexRescanManager
{
    private final Logger LOGGER = LoggerFactory.getLogger( this.getClass() );

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private ContentIndexManager contentIndexManager;

    protected KojiRemoteContenIndexingRescanManager()
    {
    }

    public void indexPreRescan( @Observes final ArtifactStorePreRescanEvent e )
            throws IndyWorkflowException
    {
        Collection<ArtifactStore> repos = e.getStores();
        for ( ArtifactStore repo : repos )
        {
            if ( repo.getType() == StoreType.remote && repo.getName()
                                                           .startsWith( KOJI_ORIGIN ) )
            {
                LOGGER.trace( "Clear content index for koji remote: {}", repo.getKey() );
                contentIndexManager.clearAllIndexedPathInStore( repo );
                contentIndexManager.clearAllIndexedPathWithOriginalStore( repo );
            }
        }
    }

    public void indexPostRescan( @Observes final ArtifactStorePostRescanEvent e )
            throws IndyWorkflowException
    {
        Collection<ArtifactStore> repos = e.getStores();
        for ( ArtifactStore repo : repos )
        {
            if ( repo.getType() == StoreType.remote && repo.getName()
                                                           .startsWith( KOJI_ORIGIN ) )
            {
                LOGGER.trace( "Rebuild content index for koji remote: {}", repo.getKey() );
                final RemoteRepository kojiRemote = (RemoteRepository) repo;
                try
                {
                    Set<Group> affected = storeDataManager.query().getGroupsAffectedBy( kojiRemote.getKey() );
                    Set<StoreKey> affetctedGroupKeys =
                            affected.stream().map( g -> g.getKey() ).collect( Collectors.toSet() );
                    StoreKey[] gKeys = affetctedGroupKeys.toArray( new StoreKey[affetctedGroupKeys.size()] );
                    kojiRemote.getPathMaskPatterns()
                              .forEach( path -> contentIndexManager.indexPathInStores( path, kojiRemote.getKey(),
                                                                                       gKeys ) );
                }
                catch ( IndyDataException ex )
                {
                    LOGGER.error( String.format( "Can not get the affected groups for hosted repo %s due to %s",
                                                 kojiRemote.getKey(), ex.getMessage() ), ex );
                }
            }
        }
    }
}
