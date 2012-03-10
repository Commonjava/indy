package org.commonjava.aprox.core.change;

import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.aprox.core.change.sl.ExpirationConstants.APROX_EVENT;
import static org.commonjava.aprox.core.change.sl.ExpirationConstants.APROX_FILE_EVENT;
import static org.commonjava.aprox.core.change.sl.ExpirationConstants.NON_CACHED_TIMEOUT;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.aprox.core.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.core.change.event.FileStorageEvent;
import org.commonjava.aprox.core.change.event.FileStorageEvent.Type;
import org.commonjava.aprox.core.change.event.ProxyManagerDeleteEvent;
import org.commonjava.aprox.core.change.event.ProxyManagerUpdateType;
import org.commonjava.aprox.core.change.sl.LoggingMatcher;
import org.commonjava.aprox.core.change.sl.MaxTimeoutMatcher;
import org.commonjava.aprox.core.change.sl.SnapshotFilter;
import org.commonjava.aprox.core.change.sl.StoreMatcher;
import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.aprox.core.rest.util.ArtifactPathInfo;
import org.commonjava.aprox.core.rest.util.ArtifactPathInfo.SnapshotInfo;
import org.commonjava.aprox.core.rest.util.PathRetriever;
import org.commonjava.shelflife.expire.ExpirationEvent;
import org.commonjava.shelflife.expire.ExpirationEventType;
import org.commonjava.shelflife.expire.ExpirationManager;
import org.commonjava.shelflife.expire.ExpirationManagerException;
import org.commonjava.shelflife.expire.match.AndMatcher;
import org.commonjava.shelflife.model.Expiration;
import org.commonjava.shelflife.model.ExpirationKey;
import org.commonjava.util.logging.Logger;

@Singleton
public class FileDeletionManager
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ExpirationManager expirationManager;

    @Inject
    private PathRetriever pathRetriever;

    @Inject
    private ProxyDataManager dataManager;

    public void onExpirationEvent( @Observes final ExpirationEvent event )
    {
        final ExpirationEventType type = event.getType();
        if ( type == ExpirationEventType.EXPIRE && isAproxFileExpirationEvent( event ) )
        {
            final StoreKey key = getStoreKey( event.getExpiration()
                                                   .getKey() );
            final String path = (String) event.getExpiration()
                                              .getData();

            final File toDelete = pathRetriever.formatStorageReference( key, path );
            if ( toDelete.exists() )
            {
                try
                {
                    logger.info( "[EXPIRED; DELETE] %s", toDelete );
                    FileUtils.forceDelete( toDelete );
                }
                catch ( final IOException e )
                {
                    logger.error( "Failed to delete expired file: %s. Reason: %s", e, toDelete.getAbsolutePath(),
                                  e.getMessage() );
                }

                cleanMetadata( key, path );
            }

            if ( ArtifactPathInfo.isSnapshot( path ) )
            {
                updateSnapshotVersions( key, path );
            }
        }
    }

    public void onFileStorageEvent( @Observes final FileStorageEvent event )
    {
        final Type type = event.getType();

        switch ( type )
        {
            case UPLOAD:
            {
                cleanMetadata( event.getStore()
                                    .getKey(), event.getPath() );

                setSnapshotTimeouts( event );
                break;
            }
            case DOWNLOAD:
            {
                cleanMetadata( event.getStore()
                                    .getKey(), event.getPath() );

                setProxyTimeouts( event );
                break;
            }
            case GENERATE:
            {
                break;
            }
        }

    }

    public void onStoreUpdate( @Observes final ArtifactStoreUpdateEvent event )
    {
        final ProxyManagerUpdateType eventType = event.getType();
        if ( eventType == ProxyManagerUpdateType.ADD_OR_UPDATE )
        {
            for ( final ArtifactStore store : event )
            {
                final StoreKey key = store.getKey();
                final StoreType type = key.getType();
                if ( type == StoreType.deploy_point )
                {
                    logger.info( "[ADJUST TIMEOUTS] Adjusting snapshot expirations in: %s", store.getKey() );
                    rescheduleSnapshotTimeouts( (DeployPoint) store );
                }
                else if ( type == StoreType.repository )
                {
                    logger.info( "[ADJUST TIMEOUTS] Adjusting proxied-file expirations in: %s", store.getKey() );
                    rescheduleProxyTimeouts( (Repository) store );
                }
            }
        }
    }

    private void rescheduleSnapshotTimeouts( final DeployPoint deploy )
    {
        long timeout = -1;
        if ( deploy.isAllowSnapshots() && deploy.getSnapshotTimeoutSeconds() > 0 )
        {
            timeout = deploy.getSnapshotTimeoutSeconds() * 1000;
        }

        if ( timeout > 0 )
        {
            final LoggingMatcher matcher = cancelAboveTimeoutForStore( deploy, timeout );

            final Set<String> allFiles = listAllFiles( deploy, new SnapshotFilter() );
            if ( matcher != null )
            {
                for ( final Expiration exp : matcher.getNonMatching() )
                {
                    allFiles.remove( exp.getData() );
                }
            }

            for ( final String path : allFiles )
            {
                try
                {
                    expirationManager.schedule( createAproxFileExpiration( deploy, path, timeout ) );
                }
                catch ( final ExpirationManagerException e )
                {
                    logger.error( "Failed to schedule expiration of: %s in %s. Reason: %s", e, path, deploy.getKey(),
                                  e.getMessage() );
                    break;
                }
            }
        }
    }

    private void rescheduleProxyTimeouts( final Repository repo )
    {
        long timeout = -1;
        if ( repo.isCached() && repo.getCacheTimeoutSeconds() > 0 )
        {
            timeout = repo.getCacheTimeoutSeconds() * 1000;
        }
        else if ( !repo.isCached() )
        {
            timeout = NON_CACHED_TIMEOUT;
        }

        if ( timeout > 0 )
        {
            final LoggingMatcher matcher = cancelAboveTimeoutForStore( repo, timeout );

            final Set<String> allFiles = listAllFiles( repo );
            if ( matcher != null )
            {
                for ( final Expiration exp : matcher.getNonMatching() )
                {
                    allFiles.remove( exp.getData() );
                }
            }

            for ( final String path : allFiles )
            {
                try
                {
                    expirationManager.schedule( createAproxFileExpiration( repo, path, timeout ) );
                }
                catch ( final ExpirationManagerException e )
                {
                    logger.error( "Failed to schedule expiration of: %s in %s. Reason: %s", e, path, repo.getKey(),
                                  e.getMessage() );
                    break;
                }
            }
        }
    }

    private Set<String> listAllFiles( final ArtifactStore store )
    {
        return listAllFiles( store, null );
    }

    private Set<String> listAllFiles( final ArtifactStore store, final FilenameFilter filter )
    {
        final File storeRoot = pathRetriever.getStoreRootDirectory( store.getKey() );
        final Set<String> paths = new HashSet<String>();
        listAll( storeRoot, "", paths, filter );

        return paths;
    }

    private void listAll( final File dir, final String parentPath, final Set<String> capturedFiles,
                          final FilenameFilter filter )
    {
        final String[] files = dir.list();
        for ( final String file : files )
        {
            if ( filter == null || filter.accept( dir, file ) )
            {
                final File f = new File( dir, file );

                final String childPath = new File( parentPath, file ).getPath();
                if ( f.isDirectory() )
                {
                    listAll( f, childPath, capturedFiles, filter );
                }
                else
                {
                    capturedFiles.add( childPath );
                }
            }
        }
    }

    private LoggingMatcher cancelAboveTimeoutForStore( final ArtifactStore store, final long timeout )
    {
        final StoreKey key = store.getKey();
        try
        {
            final LoggingMatcher matcher = new LoggingMatcher( new MaxTimeoutMatcher( timeout ) );

            expirationManager.cancelAll( new AndMatcher( new StoreMatcher( key ), matcher ) );

            return matcher;
        }
        catch ( final ExpirationManagerException e )
        {
            logger.error( "Failed to cancel (for purposes of rescheduling) expirations for store: %s. Reason: %s", e,
                          key, e.getMessage() );
        }

        return null;
    }

    public void onStoreDeletion( @Observes final ProxyManagerDeleteEvent event )
    {
        final StoreType type = event.getType();
        final Collection<String> names = event.getNames();

        for ( final String name : names )
        {
            final StoreKey key = new StoreKey( type, name );
            final File dir = pathRetriever.getStoreRootDirectory( key );
            if ( dir.exists() && dir.isDirectory() )
            {
                try
                {
                    logger.info( "[STORE REMOVED; DELETE] %s", dir );
                    forceDelete( dir );
                    expirationManager.cancelAll( new StoreMatcher( key ) );
                }
                catch ( final IOException e )
                {
                    logger.error( "Failed to delete storage for deleted artifact store: %s (dir: %s). Error: %s", e,
                                  key, dir, e.getMessage() );
                }
                catch ( final ExpirationManagerException e )
                {
                    logger.error( "Failed to cancel file expirations for deleted artifact store: %s (dir: %s). Error: %s",
                                  e, key, dir, e.getMessage() );
                }
            }
        }
    }

    private void setProxyTimeouts( final FileStorageEvent event )
    {
        final Repository repo = (Repository) event.getStore();
        final String path = event.getPath();

        long timeout = NON_CACHED_TIMEOUT;
        if ( repo.isCached() )
        {
            timeout = repo.getCacheTimeoutSeconds() * 1000;
        }

        if ( timeout > 0 )
        {
            logger.info( "[PROXY TIMEOUT SET] %s/%s; %s", repo.getKey(), path, new Date( timeout ) );
            try
            {
                expirationManager.schedule( createAproxFileExpiration( repo, path, timeout ) );
            }
            catch ( final ExpirationManagerException e )
            {
                logger.error( "Failed to schedule expiration of path: %s\n  Store: %s\n  Timeout: %s\n  Error: %s", e,
                              path, repo, timeout, e.getMessage() );
            }
        }
    }

    private void setSnapshotTimeouts( final FileStorageEvent event )
    {
        final DeployPoint deploy = (DeployPoint) event.getStore();
        final String path = event.getPath();

        if ( ArtifactPathInfo.isSnapshot( path ) && deploy.getSnapshotTimeoutSeconds() > 0 )
        {
            final long timeout = deploy.getSnapshotTimeoutSeconds() * 1000;
            logger.info( "[SNAPSHOT TIMEOUT SET] %s/%s; %s", deploy.getKey(), path, new Date( timeout ) );
            try
            {
                expirationManager.schedule( createAproxFileExpiration( deploy, path, timeout ) );
            }
            catch ( final ExpirationManagerException e )
            {
                logger.error( "Failed to schedule expiration of path: %s\n  Store: %s\n  Timeout: %s\n  Error: %s", e,
                              path, deploy, timeout, e.getMessage() );
            }
        }
    }

    private void cleanMetadata( final StoreKey key, final String path )
    {
        if ( path.endsWith( "maven-metadata.xml" ) )
        {
            try
            {
                final Set<Group> groups = dataManager.getGroupsContaining( key );
                for ( final Group group : groups )
                {
                    logger.info( "[CLEAN] Cleaning metadata path: %s in group: %s", path, group.getName() );
                    final File md = pathRetriever.formatStorageReference( group, path );
                    if ( md.exists() )
                    {
                        md.delete();
                    }
                }
            }
            catch ( final ProxyDataException e )
            {
                logger.error( "Attempting to update groups for metadata change; Failed to retrieve groups containing store: %s. Error: %s",
                              e, key, e.getMessage() );
            }
        }
    }

    private StoreKey getStoreKey( final ExpirationKey key )
    {
        final String[] parts = key.getParts();
        if ( parts.length < 5 )
        {
            return null;
        }
        else
        {
            return new StoreKey( StoreType.valueOf( parts[2] ), parts[3] );
        }
    }

    private Expiration createAproxFileExpiration( final ArtifactStore store, final String path, final long timeout )
    {
        final String pathHash = DigestUtils.md5Hex( path );
        return new Expiration( new ExpirationKey( APROX_EVENT, APROX_FILE_EVENT, store.getKey()
                                                                                      .getType()
                                                                                      .name(), store.getKey()
                                                                                                    .getName(),
                                                  pathHash ), System.currentTimeMillis() + timeout, path );
    }

    private boolean isAproxFileExpirationEvent( final ExpirationEvent event )
    {
        return new StoreMatcher().matches( event.getExpiration() );
    }

    private void updateSnapshotVersions( final StoreKey key, final String path )
    {
        final ArtifactPathInfo pathInfo = pathRetriever.parsePathInfo( path );

        final File f = pathRetriever.formatStorageReference( key, path );
        if ( f.getParentFile() == null || f.getParentFile()
                                           .getParentFile() == null )
        {
            return;
        }

        final File metadata = new File( f.getParentFile()
                                         .getParentFile(), "maven-metadata.xml" );
        if ( metadata.exists() )
        {
            logger.info( "[UPDATE VERSIONS] Updating snapshot versions for path: %s in store: %s", path, key.getName() );
            FileReader reader = null;
            FileWriter writer = null;
            try
            {
                reader = new FileReader( metadata );
                final Metadata md = new MetadataXpp3Reader().read( reader );

                final Versioning versioning = md.getVersioning();
                final List<String> versions = versioning.getVersions();

                final String version = pathInfo.getVersion();
                String replacement = null;

                final int idx = versions.indexOf( version );
                if ( idx > -1 )
                {
                    if ( idx > 0 )
                    {
                        replacement = versions.get( idx - 1 );
                    }

                    versions.remove( idx );
                }

                if ( version.equals( versioning.getLatest() ) )
                {
                    versioning.setLatest( replacement );
                }

                final SnapshotInfo si = ArtifactPathInfo.parseSnapshotInfo( pathInfo.getVersion() );
                if ( si != null )
                {
                    final SnapshotInfo siRepl = ArtifactPathInfo.parseSnapshotInfo( replacement );
                    final Snapshot snapshot = versioning.getSnapshot();
                    if ( si.getTimestamp()
                           .equals( snapshot.getTimestamp() ) && si.getBuildNumber() == snapshot.getBuildNumber() )
                    {
                        if ( siRepl != null )
                        {
                            snapshot.setTimestamp( siRepl.getTimestamp() );
                            snapshot.setBuildNumber( siRepl.getBuildNumber() );
                        }
                        else
                        {
                            versioning.setSnapshot( null );
                        }
                    }
                }

                writer = new FileWriter( metadata );
            }
            catch ( final IOException e )
            {
                logger.error( "Failed to update metadata after snapshot deletion.\n  Snapshot: %s\n  Metadata: %s\n  Reason: %s",
                              e, f, metadata, e.getMessage() );
            }
            catch ( final XmlPullParserException e )
            {
                logger.error( "Failed to update metadata after snapshot deletion.\n  Snapshot: %s\n  Metadata: %s\n  Reason: %s",
                              e, f, metadata, e.getMessage() );
            }
            finally
            {
                closeQuietly( reader );
                closeQuietly( writer );
            }
        }
    }

}
