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
package org.commonjava.indy.content.index.conf;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.change.event.ArtifactStorePostRescanEvent;
import org.commonjava.indy.change.event.ArtifactStorePreRescanEvent;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.content.index.ContentIndexManager;
import org.commonjava.indy.content.index.ContentIndexRescanManager;
import org.commonjava.indy.content.index.IndexedStorePath;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This component will handle the content index for the hosted repo during its rescan. It will rebuild all relevant
 * index entries for the hosted repo, and remove all useless entries for the missed artifacts in hosted repo
 */
@ApplicationScoped
public class HostedContentIndexRescanManager implements ContentIndexRescanManager
{
    private final Logger LOGGER = LoggerFactory.getLogger( this.getClass() );

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private ContentIndexManager contentIndexManager;

    @Inject
    private DownloadManager downloadManager;

    protected HostedContentIndexRescanManager()
    {
    }

    public void indexPreRescan( @Observes final ArtifactStorePreRescanEvent e )
            throws IndyWorkflowException
    {
        Collection<ArtifactStore> affectedRepos = e.getStores();
        for ( ArtifactStore repo : affectedRepos )
        {
            if ( repo.getType() == StoreType.hosted )
            {
                final HostedRepository hosted = (HostedRepository) repo;

                LOGGER.trace( "Clear content index for {}", hosted.getKey() );
                // Remove the content index items for the hosted which will be rescanned
                contentIndexManager.clearAllIndexedPathInStore( hosted );
                // Remove the content index items for the affected groups of the hosted which will be rescanned, note that
                // we will only cared about the items that is from this hosted only but not others(the origin key in
                // IndexedStorePath which hits this hosted)
                contentIndexManager.clearAllIndexedPathWithOriginalStore( hosted );
            }
        }
    }

    public void indexPostRescan( @Observes final ArtifactStorePostRescanEvent e )
            throws IndyWorkflowException
    {
        Collection<ArtifactStore> hostedStores = e.getStores();
        for ( ArtifactStore repo : hostedStores )
        {
            if ( repo.getType() == StoreType.hosted )
            {
                LOGGER.trace( "Rebuild content index for {}", repo.getKey() );
                final HostedRepository hosted = (HostedRepository) repo;
                try
                {
                    List<Transfer> transfers = downloadManager.listRecursively( hosted.getKey(), DownloadManager.ROOT_PATH );
                    Set<Group> affected = storeDataManager.query().getGroupsAffectedBy( hosted.getKey() );
                    Set<StoreKey> affetctedGroupKeys =
                            affected.stream().map( g -> g.getKey() ).collect( Collectors.toSet() );
                    StoreKey[] gKeys = affetctedGroupKeys.toArray( new StoreKey[affetctedGroupKeys.size()] );
                    transfers.forEach(
                            txfr -> contentIndexManager.indexPathInStores( txfr.getPath(), hosted.getKey(), gKeys ) );
                }
                catch ( IndyWorkflowException ex )
                {
                    LOGGER.error( String.format( "Can not list resource correctly for hosted repo %s due to %s",
                                                 hosted.getKey(), ex.getMessage() ), ex );
                }
                catch ( IndyDataException ex )
                {
                    LOGGER.error( String.format( "Can not get the affected groups for hosted repo %s due to %s",
                                                 hosted.getKey(), ex.getMessage() ), ex );
                }
            }
        }
    }
}
