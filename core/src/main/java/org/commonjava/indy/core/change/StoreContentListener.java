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
package org.commonjava.indy.core.change;

import org.commonjava.cdi.util.weft.DrainingExecutorCompletionService;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.change.event.ArtifactStoreDeletePreEvent;
import org.commonjava.indy.change.event.ArtifactStoreEnablementEvent;
import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.content.StoreContentAction;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.commonjava.indy.change.event.ArtifactStoreUpdateType.UPDATE;
import static org.commonjava.indy.core.change.StoreChangeUtil.delete;
import static org.commonjava.indy.core.change.StoreChangeUtil.getDiffMembers;
import static org.commonjava.indy.core.change.StoreChangeUtil.listPathsAnd;
import static org.commonjava.indy.model.core.StoreType.group;

/**
 * Created by jdcasey on 1/27/17.
 */
@ApplicationScoped
public class StoreContentListener
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private ContentCleanupHelper contentCleanupHelper;

    /**
     * Handles store disable/enablement.
     */
    @Measure
    public void onStoreEnablement( @Observes final ArtifactStoreEnablementEvent event )
    {
        logger.trace( "Got store-enablement event: {}", event );
        if ( event.isPreprocessing() )
        {
            Set<StoreKey> keys = event.getStores().stream().map( ArtifactStore::getKey ).collect( Collectors.toSet() );
            contentCleanupHelper.clearPaths( keys, contentCleanupHelper.mergablePath(), false );
        }
    }

    @Measure
    public void onStoreDeletion( @Observes final ArtifactStoreDeletePreEvent event )
    {
        logger.trace( "Got store-delete event: {}", event );
        Set<StoreKey> keys = event.getStores().stream().map( ArtifactStore::getKey ).collect( Collectors.toSet() );
        contentCleanupHelper.clearPaths( keys, contentCleanupHelper.allPath(), true );
    }

    @Measure
    public void onStoreUpdate( @Observes final ArtifactStorePreUpdateEvent event )
    {
        logger.trace( "Got store-update event: {}", event );
        for ( ArtifactStore store : event )
        {
            // we're only interested in groups, since only adjustments to group memberships can invalidate cached content.
            if ( group == store.getKey().getType() )
            {
                if ( event.getType() == UPDATE )
                {
                    cleanSupercededMemberContent( (Group) store, event.getChangeMap() );
                }
            }
        }
    }

    /**
     * Get the added and removed members and clear mergable paths for added and all paths for removed.
     * We don't cache normal files in groups but need to cleaning NFC, content-index, etc, via storeContentActions.
     */
    private void cleanSupercededMemberContent( final Group group,
                                               final Map<ArtifactStore, ArtifactStore> changeMap )
    {
        Set<StoreKey>[] diffMembers = getDiffMembers( group, (Group) changeMap.get( group ) );

        Set<StoreKey> added = diffMembers[0];
        Set<StoreKey> removed = diffMembers[1];

        logger.debug( "Diff members, added: {}, removed: {}", added, removed );

        Set<Group> groups = new HashSet<>();
        groups.add( group );

        try
        {
            groups.addAll( storeDataManager.query()
                                           .packageType( group.getPackageType() )
                                           .getGroupsAffectedBy( group.getKey() ) );
        }
        catch ( IndyDataException e )
        {
            logger.error( "Failed to retrieve groups affected by: {}", group.getKey(), e );
        }

        logger.debug( "Affected groups: {}", groups );

        final boolean deleteOriginPath = false;

        contentCleanupHelper.clearPaths( added, contentCleanupHelper.mergablePath(), groups, deleteOriginPath );
        contentCleanupHelper.clearPaths( removed, contentCleanupHelper.allPath(), groups, deleteOriginPath );
    }

}
