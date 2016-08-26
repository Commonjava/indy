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
package org.commonjava.indy.implrepo.data;

import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.implrepo.conf.ImpliedRepoConfig;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.commonjava.indy.model.core.ArtifactStore.METADATA_ORIGIN;

/**
 * Wrap methods that retrieve stores for a group, or groups containing a store. Check if the store(s) in question are
 * implied (created by implied-repos), and whether the group has implied repositories enabled. If the two don't match,
 * filter the results appropriately to keep implied repos out of groups that don't support them.
 *
 * Created by jdcasey on 8/17/16.
 */
@Decorator
public abstract class ImpliedReposStoreDataManagerDecorator
        implements StoreDataManager
{
    public static final String IMPLIED_REPO_ORIGIN = "implied-repos";

    @Delegate
    @Inject
    private StoreDataManager delegate;

    @Inject
    private ImpliedRepoConfig config;

    @Override
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( String groupName, boolean enabledOnly )
            throws IndyDataException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Retrieving ordered concrete (recursive) members for group: {}", groupName );

        List<ArtifactStore> result = delegate.getOrderedConcreteStoresInGroup( groupName, enabledOnly );
        if ( logger.isTraceEnabled() )
        {
            logger.trace( "Raw ordered concrete membership for group: {} is:\n  {}", groupName, StringUtils.join(result, "\n  ") );
        }

        result = maybeFilter( groupName, result );
        if ( logger.isTraceEnabled() )
        {
            logger.trace( "Filtered for implied-repos: ordered concrete membership for group: {} is now:\n  {}", groupName, StringUtils.join(result, "\n  ") );
        }

        return result;
    }

    @Override
    public List<ArtifactStore> getOrderedStoresInGroup( String groupName, boolean enabledOnly )
            throws IndyDataException
    {
        List<ArtifactStore> delegateResult = delegate.getOrderedStoresInGroup( groupName, enabledOnly );

        return maybeFilter( groupName, delegateResult );
    }

    private List<ArtifactStore> maybeFilter( String groupName, List<ArtifactStore> delegateResult )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        if ( delegateResult == null || delegateResult.isEmpty() || config.isEnabledForGroup( groupName ) )
        {
            logger.trace( "Implied repositories are enabled for group: '{}'. Returning all membership from delegate result.", groupName );
            return delegateResult;
        }

        logger.trace( "Filtering stores with metadata: '{}' value of '{}' from membership results", METADATA_ORIGIN, IMPLIED_REPO_ORIGIN );
        List<ArtifactStore> result = new ArrayList<>();
        delegateResult.stream()
                      .filter( ( store ) -> !IMPLIED_REPO_ORIGIN.equals( store.getMetadata( METADATA_ORIGIN ) ) )
                      .forEach( ( store ) -> result.add( store ) );

        return result;
    }

    @Override
    public Set<Group> getGroupsContaining( StoreKey key )
            throws IndyDataException
    {
        ArtifactStore store = delegate.getArtifactStore( key );
        if ( store == null )
        {
            return Collections.emptySet();
        }

        boolean storeIsImplied = IMPLIED_REPO_ORIGIN.equals( store.getMetadata( METADATA_ORIGIN ) );
        Set<Group> delegateGroups = delegate.getGroupsContaining( key );
        if ( !storeIsImplied || delegateGroups == null || delegateGroups.isEmpty() )
        {
            return delegateGroups;
        }

        Set<Group> result = new HashSet<>();
        delegateGroups.stream()
                      .filter( ( group ) -> config.isEnabledForGroup( group.getName() ) )
                      .forEach( ( group ) -> result.add( group ) );

        return result;
    }
}
