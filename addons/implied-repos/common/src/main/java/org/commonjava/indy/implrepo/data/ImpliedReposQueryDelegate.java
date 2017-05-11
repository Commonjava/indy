package org.commonjava.indy.implrepo.data;

import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.data.DelegatingArtifactStoreQuery;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.ArtifactStoreQuery;
import org.commonjava.indy.implrepo.conf.ImpliedRepoConfig;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.commonjava.indy.implrepo.data.ImpliedReposStoreDataManagerDecorator.IMPLIED_REPO_ORIGIN;
import static org.commonjava.indy.model.core.ArtifactStore.METADATA_ORIGIN;

/**
 * Created by jdcasey on 5/11/17.
 */
public class ImpliedReposQueryDelegate
        extends DelegatingArtifactStoreQuery<ArtifactStore>
{
    private final ImpliedReposStoreDataManagerDecorator dataManager;

    private final ImpliedRepoConfig config;

    public ImpliedReposQueryDelegate( final ArtifactStoreQuery<ArtifactStore> query,
                                      final ImpliedReposStoreDataManagerDecorator dataManager,
                                      final ImpliedRepoConfig config )
    {
        super( query );
        this.dataManager = dataManager;
        this.config = config;
    }

    @Override
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( String groupName )
            throws IndyDataException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Retrieving ordered concrete (recursive) members for group: {}", groupName );

        List<ArtifactStore> result = delegate().getOrderedConcreteStoresInGroup( groupName );
        if ( logger.isTraceEnabled() )
        {
            logger.trace( "Raw ordered concrete membership for group: {} is:\n  {}", groupName,
                          StringUtils.join( result, "\n  " ) );
        }

        result = maybeFilter( groupName, result );
        if ( logger.isTraceEnabled() )
        {
            logger.trace( "Filtered for implied-repos: ordered concrete membership for group: {} is now:\n  {}",
                          groupName, StringUtils.join( result, "\n  " ) );
        }

        return result;
    }

    @Override
    public List<ArtifactStore> getOrderedStoresInGroup( String groupName )
            throws IndyDataException
    {
        List<ArtifactStore> delegateResult = delegate().getOrderedStoresInGroup( groupName );

        return maybeFilter( groupName, delegateResult );
    }

    private List<ArtifactStore> maybeFilter( String groupName, List<ArtifactStore> delegateResult )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        if ( delegateResult == null || delegateResult.isEmpty() || config.isEnabledForGroup( groupName ) )
        {
            logger.trace(
                    "Implied repositories are enabled for group: '{}'. Returning all membership from delegate result.",
                    groupName );
            return delegateResult;
        }

        logger.trace( "Filtering stores with metadata: '{}' value of '{}' from membership results", METADATA_ORIGIN,
                      IMPLIED_REPO_ORIGIN );
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
        ArtifactStore store = dataManager.getArtifactStore( key );
        if ( store == null )
        {
            return Collections.emptySet();
        }

        boolean storeIsImplied = IMPLIED_REPO_ORIGIN.equals( store.getMetadata( METADATA_ORIGIN ) );
        Set<Group> delegateGroups = delegate().getGroupsContaining( key );
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
