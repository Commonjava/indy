package org.commonjava.aprox.indexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.maven.index.Indexer;
import org.apache.maven.index.context.ExistingLuceneIndexMismatchException;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.StaticContextMemberProvider;
import org.apache.maven.index.creator.JarFileContentsIndexCreator;
import org.apache.maven.index.creator.MavenArchetypeArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MavenPluginArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.commonjava.aprox.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.change.event.FileStorageEvent;
import org.commonjava.aprox.change.event.ProxyManagerDeleteEvent;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.shelflife.ExpirationManager;
import org.commonjava.shelflife.ExpirationManagerException;
import org.commonjava.shelflife.event.ExpirationEvent;
import org.commonjava.shelflife.model.Expiration;
import org.commonjava.shelflife.model.ExpirationKey;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class IndexHandler
{

    public static final long GROUP_INDEX_TIMEOUT = TimeUnit.MILLISECONDS.convert( 24, TimeUnit.HOURS );

    public static final long DEPLOY_POINT_INDEX_TIMEOUT = TimeUnit.MILLISECONDS.convert( 10, TimeUnit.MINUTES );

    public static final String INDEX_KEY_PREFIX = "aprox-index";

    private static final String INDEX_DIR = "/.index";

    private final Logger logger = new Logger( getClass() );

    private DefaultPlexusContainer plexusContainer;

    private Indexer indexer;

    private IndexUpdater indexUpdater;

    private List<IndexCreator> indexCreators;

    @Inject
    private ExpirationManager expirationManager;

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private FileManager fileManager;

    public IndexHandler()
    {
    }

    public IndexHandler( final ExpirationManager expirationManager, final StoreDataManager storeDataManager,
                         final FileManager fileManager )
        throws AproxIndexerException
    {
        this.expirationManager = expirationManager;
        this.storeDataManager = storeDataManager;
        this.fileManager = fileManager;
        setup();
    }

    @PostConstruct
    public void setup()
        throws AproxIndexerException
    {
        try
        {
            plexusContainer = new DefaultPlexusContainer();

            // lookup the indexer components from plexus
            indexer = plexusContainer.lookup( Indexer.class );
            indexUpdater = plexusContainer.lookup( IndexUpdater.class );

            // Creators we want to use (search for fields it defines)
            indexCreators = new ArrayList<IndexCreator>( 4 );
            indexCreators.add( plexusContainer.lookup( IndexCreator.class, MinimalArtifactInfoIndexCreator.ID ) );
            indexCreators.add( plexusContainer.lookup( IndexCreator.class, JarFileContentsIndexCreator.ID ) );
            indexCreators.add( plexusContainer.lookup( IndexCreator.class, MavenPluginArtifactInfoIndexCreator.ID ) );
            indexCreators.add( plexusContainer.lookup( IndexCreator.class, MavenArchetypeArtifactInfoIndexCreator.ID ) );

            indexCreators = Collections.unmodifiableList( indexCreators );
        }
        catch ( final ComponentLookupException e )
        {
            throw new AproxIndexerException( "Failed to lookup components for indexer setup: %s", e, e.getMessage() );
        }
        catch ( final PlexusContainerException e )
        {
            throw new AproxIndexerException( "Failed to start plexus container for indexer setup: %s", e,
                                             e.getMessage() );
        }
        finally
        {
        }
    }

    public void onDelete( @Observes final ProxyManagerDeleteEvent event )
    {
        final StoreType type = event.getType();
        if ( type != StoreType.group )
        {
            final Set<Group> updated = new HashSet<Group>();
            for ( final String name : event )
            {
                updateGroupsFor( new StoreKey( type, name ), updated );
            }
        }
        else
        {
            for ( final String name : event )
            {
                try
                {
                    expirationManager.cancel( expirationForGroup( name ) );
                }
                catch ( final ExpirationManagerException e )
                {
                    logger.error( "Failed to cancel indexer trigger for group: %s. Reason: %s", e, name, e.getMessage() );
                }
            }
        }
    }

    public void onStorage( @Observes final FileStorageEvent event )
    {
        final StorageItem item = event.getStorageItem();
        final StoreKey key = item.getStoreKey();
        final String path = item.getPath();
        if ( !isIndexable( path ) )
        {
            return;
        }

        if ( key.getType() == StoreType.deploy_point )
        {
            DeployPoint store = null;
            try
            {
                store = storeDataManager.getDeployPoint( key.getName() );
            }
            catch ( final ProxyDataException e )
            {
                logger.error( "Failed to retrieve deploy-point for index update: %s. Reason: %s", e, key,
                              e.getMessage() );
            }

            if ( store != null )
            {
                final Expiration exp = expirationForDeployPoint( key.getName() );
                try
                {
                    if ( !expirationManager.contains( exp ) )
                    {
                        expirationManager.schedule( exp );
                    }
                }
                catch ( final ExpirationManagerException e )
                {
                    logger.error( "Failed to schedule index update for deploy-point: %s. Reason: %s", e, key,
                                  e.getMessage() );
                }
            }
        }
    }

    public void onExpire( @Observes final ExpirationEvent event )
    {
        final Expiration expiration = event.getExpiration();
        final String[] parts = expiration.getKey()
                                         .getParts();
        if ( !INDEX_KEY_PREFIX.equals( parts[0] ) )
        {
            return;
        }

        final StoreKey key = StoreKey.fromString( (String) expiration.getData() );
        final StoreType type = key.getType();

        ArtifactStore store;
        try
        {
            store = storeDataManager.getArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to update index for: %s. Reason: %s", e, key, e.getMessage() );
            return;
        }

        if ( type == StoreType.deploy_point )
        {
            updateDeployPointIndex( (DeployPoint) store );
        }
        else if ( type == StoreType.group )
        {
            updateMergedIndex( (Group) store, new HashSet<Group>() );
        }
    }

    public void onAdd( @Observes final ArtifactStoreUpdateEvent event )
    {
        final Set<Group> updated = new HashSet<Group>();
        for ( final ArtifactStore store : event )
        {
            if ( store.getKey()
                      .getType() == StoreType.group )
            {
                final Group group = (Group) store;
                if ( updated.contains( group ) )
                {
                    continue;
                }

                updateMergedIndex( group, updated );
            }
            else
            {
                if ( store.getKey()
                          .getType() == StoreType.deploy_point )
                {
                    updateDeployPointIndex( (DeployPoint) store );
                }

                updateGroupsFor( store.getKey(), updated );
            }
        }
    }

    private boolean isIndexable( final String path )
    {
        if ( path.endsWith( ".sha1" ) || path.endsWith( ".md5" ) || path.endsWith( "maven-metadata.xml" )
            || path.endsWith( "archetype-catalog.xml" ) )
        {
            return false;
        }

        return true;
    }

    private void updateDeployPointIndex( final DeployPoint store )
    {
        final File indexDir = fileManager.getStorageReference( store, INDEX_DIR )
                                         .getDetachedFile();

        final File indexCache = fileManager.getStorageReference( store, FileManager.ROOT_PATH )
                                           .getDetachedFile();

        final String id = store.getKey()
                               .toString();

        IndexingContext context = null;
        try
        {
            context = indexer.createIndexingContext( id, id, indexCache, indexDir, id, null, true, true, indexCreators );
        }
        catch ( final ExistingLuceneIndexMismatchException e )
        {
            logger.error( "Failed to indexing context for deploy-point: %s. Reason: %s", e, store.getName(),
                          e.getMessage() );
        }
        catch ( final IllegalArgumentException e )
        {
            logger.error( "Failed to indexing context for deploy-point: %s. Reason: %s", e, store.getName(),
                          e.getMessage() );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to indexing context for deploy-point: %s. Reason: %s", e, store.getName(),
                          e.getMessage() );
        }

        if ( context == null )
        {
            return;
        }

        doIndexUpdate( context, store );
    }

    private void updateGroupsFor( final StoreKey storeKey, final Set<Group> updated )
    {
        try
        {
            final Set<Group> groups = storeDataManager.getGroupsContaining( storeKey );
            if ( groups != null )
            {
                for ( final Group group : groups )
                {
                    if ( updated.contains( group ) )
                    {
                        continue;
                    }

                    updateMergedIndex( group, updated );
                }
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve groups that contain: %s. Reason: %s", e, storeKey, e.getMessage() );
        }
    }

    private void updateMergedIndex( final Group group, final Set<Group> updated )
    {
        final List<IndexingContext> contexts = getContextsFor( group, indexCreators );

        if ( contexts == null )
        {
            return;
        }

        final File rootDir = fileManager.getStorageReference( group, FileManager.ROOT_PATH )
                                        .getDetachedFile();

        final File indexDir = fileManager.getStorageReference( group, INDEX_DIR )
                                         .getDetachedFile();

        IndexingContext mergedContext = null;
        try
        {
            mergedContext =
                indexer.createMergedIndexingContext( group.getName(), group.getName(), rootDir, indexDir, true,
                                                     new StaticContextMemberProvider( contexts ) );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to create merged indexing context for group: %s. Reason: %s", e, group.getName(),
                          e.getMessage() );
            return;
        }

        doIndexUpdate( mergedContext, group );

        updated.add( group );

        try
        {
            final Expiration exp = expirationForGroup( group.getName() );
            expirationManager.schedule( exp );
            logger.info( "Next index update in group: %s scheduled for: %s", group.getName(),
                         new Date( exp.getExpires() ) );
        }
        catch ( final ExpirationManagerException e )
        {
            logger.error( "Failed to schedule indexer trigger for group: %s. Reason: %s", e, group.getName(),
                          e.getMessage() );
        }
    }

    private IndexUpdateResult doIndexUpdate( final IndexingContext mergedContext, final ArtifactStore store )
    {
        final ResourceFetcher resourceFetcher = new AproxResourceFetcher( storeDataManager, fileManager );

        final Date centralContextCurrentTimestamp = mergedContext.getTimestamp();
        final IndexUpdateRequest updateRequest = new IndexUpdateRequest( mergedContext, resourceFetcher );
        IndexUpdateResult updateResult = null;
        try
        {
            updateResult = indexUpdater.fetchAndUpdateIndex( updateRequest );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to update index for: %s. Reason: %s", store.getKey(), e.getMessage() );
        }

        if ( updateResult == null )
        {
            return null;
        }

        if ( updateResult.isFullUpdate() )
        {
            logger.info( "FULL index update completed for: %s", store.getKey() );
        }
        else if ( updateResult.getTimestamp()
                              .equals( centralContextCurrentTimestamp ) )
        {
            logger.info( "NO index update for: %s. Index is up-to-date.", store.getKey() );
        }
        else
        {
            logger.info( "INCREMENTAL index update completed for: %s to cover period: %s - %s", store.getKey(),
                         centralContextCurrentTimestamp, updateResult.getTimestamp() );
        }

        return updateResult;
    }

    private List<IndexingContext> getContextsFor( final Group group, final List<IndexCreator> indexers )
    {
        List<IndexingContext> contexts = null;
        try
        {
            final List<ArtifactStore> stores = storeDataManager.getOrderedConcreteStoresInGroup( group.getName() );
            if ( stores != null && !stores.isEmpty() )
            {
                contexts = new ArrayList<IndexingContext>( stores.size() );
                for ( final ArtifactStore store : stores )
                {
                    final File indexDir = fileManager.getStorageReference( store, INDEX_DIR )
                                                     .getDetachedFile();

                    final File indexCache = fileManager.getStorageReference( store, FileManager.ROOT_PATH )
                                                       .getDetachedFile();

                    final String id = store.getKey()
                                           .toString();
                    contexts.add( indexer.createIndexingContext( id, id, indexCache, indexDir, id, null, true, true,
                                                                 indexers ) );
                }
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve ordered concrete stores in group: %s. Reason: %s", e, group.getName(),
                          e.getMessage() );
        }
        catch ( final ExistingLuceneIndexMismatchException e )
        {
            logger.error( "Failed to indexing contexts for group: %s. Reason: %s", e, group.getName(), e.getMessage() );
        }
        catch ( final IllegalArgumentException e )
        {
            logger.error( "Failed to indexing contexts for group: %s. Reason: %s", e, group.getName(), e.getMessage() );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to indexing contexts for group: %s. Reason: %s", e, group.getName(), e.getMessage() );
        }

        return contexts;
    }

    private Expiration expirationForGroup( final String name )
    {
        return new Expiration( new ExpirationKey( StoreType.group.name(), INDEX_KEY_PREFIX, name ),
                               GROUP_INDEX_TIMEOUT, new StoreKey( StoreType.group, name ) );
    }

    private Expiration expirationForDeployPoint( final String name )
    {
        return new Expiration( new ExpirationKey( StoreType.deploy_point.name(), INDEX_KEY_PREFIX, name ),
                               DEPLOY_POINT_INDEX_TIMEOUT, new StoreKey( StoreType.deploy_point, name ) );
    }

}
