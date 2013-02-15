package org.commonjava.aprox.core.change;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.aprox.core.change.sl.ExpirationConstants.APROX_EVENT;
import static org.commonjava.aprox.core.change.sl.ExpirationConstants.APROX_FILE_EVENT;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.aprox.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.change.event.FileAccessEvent;
import org.commonjava.aprox.change.event.FileDeletionEvent;
import org.commonjava.aprox.change.event.FileEvent;
import org.commonjava.aprox.change.event.FileStorageEvent;
import org.commonjava.aprox.change.event.FileStorageEvent.Type;
import org.commonjava.aprox.change.event.ProxyManagerDeleteEvent;
import org.commonjava.aprox.change.event.ProxyManagerUpdateType;
import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.core.change.sl.LoggingMatcher;
import org.commonjava.aprox.core.change.sl.MaxTimeoutMatcher;
import org.commonjava.aprox.core.change.sl.SnapshotFilter;
import org.commonjava.aprox.core.change.sl.StoreMatcher;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.rest.util.ArtifactPathInfo;
import org.commonjava.aprox.rest.util.ArtifactPathInfo.SnapshotInfo;
import org.commonjava.shelflife.expire.ExpirationEvent;
import org.commonjava.shelflife.expire.ExpirationEventType;
import org.commonjava.shelflife.expire.ExpirationManager;
import org.commonjava.shelflife.expire.ExpirationManagerException;
import org.commonjava.shelflife.expire.match.AndMatcher;
import org.commonjava.shelflife.model.Expiration;
import org.commonjava.shelflife.model.ExpirationKey;
import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
public class TimeoutManager
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ExpirationManager expirationManager;

    @Inject
    private FileManager fileManager;

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private AproxConfiguration config;

    public void onExpirationEvent( @Observes final ExpirationEvent event )
    {
        final ExpirationEventType type = event.getType();
        if ( type == ExpirationEventType.EXPIRE )
        {
            if ( isAproxFileExpirationEvent( event ) )
            {
                final StoreKey key = getStoreKey( event.getExpiration()
                                                       .getKey() );
                final String path = (String) event.getExpiration()
                                                  .getData();

                final StorageItem toDelete = fileManager.getStorageReference( key, path );
                if ( toDelete.exists() )
                {
                    try
                    {
                        logger.info( "[EXPIRED; DELETE] %s", toDelete );
                        toDelete.delete();
                    }
                    catch ( final IOException e )
                    {
                        logger.error( "Failed to delete expired file: %s. Reason: %s", e, toDelete.getFullPath(),
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
    }

    public void onFileStorageEvent( @Observes final FileStorageEvent event )
    {
        final Type type = event.getType();

        switch ( type )
        {
            case UPLOAD:
            {
                cleanMetadata( event );

                setSnapshotTimeouts( event );
                break;
            }
            case DOWNLOAD:
            {
                cleanMetadata( event );

                setProxyTimeouts( event );
                break;
            }
            case GENERATE:
            {
                break;
            }
        }
    }

    public void onFileAccessEvent( @Observes final FileAccessEvent event )
    {
        final StoreType type = event.getStorageItem()
                                    .getStoreKey()
                                    .getType();

        if ( type == StoreType.deploy_point )
        {
            setSnapshotTimeouts( event );
        }
        else if ( type == StoreType.repository )
        {
            setProxyTimeouts( event );
        }
    }

    public void onFileDeletionEvent( @Observes final FileDeletionEvent event )
    {
        cancel( event.getStorageItem()
                     .getStoreKey(), event.getStorageItem()
                                          .getPath() );
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
        if ( !repo.isPassthrough() && repo.getCacheTimeoutSeconds() > 0 )
        {
            timeout = repo.getCacheTimeoutSeconds() * 1000;
        }
        else if ( repo.isPassthrough() )
        {
            timeout = config.getPassthroughTimeoutSeconds() * 1000;
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
        final StorageItem storeRoot = fileManager.getStoreRootDirectory( store.getKey() );
        final Set<String> paths = new HashSet<String>();
        listAll( storeRoot, "", paths, filter );

        return paths;
    }

    private void listAll( final StorageItem dir, final String parentPath, final Set<String> capturedFiles,
                          final FilenameFilter filter )
    {
        final String[] files = dir.exists() ? dir.list() : null;
        if ( files != null )
        {
            for ( final String file : files )
            {
                final File d = dir.getDetachedFile();
                if ( filter == null || filter.accept( d, file ) )
                {
                    final StorageItem child = dir.getChild( file );

                    final String childPath = new File( parentPath, file ).getPath();
                    if ( child.isDirectory() )
                    {
                        listAll( child, childPath, capturedFiles, filter );
                    }
                    else
                    {
                        capturedFiles.add( childPath );
                    }
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
            final StorageItem dir = fileManager.getStoreRootDirectory( key );
            if ( dir.exists() && dir.isDirectory() )
            {
                try
                {
                    logger.info( "[STORE REMOVED; DELETE] %s", dir.getFullPath() );
                    dir.delete();
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

    private void setProxyTimeouts( final FileEvent event )
    {
        Repository repo = null;
        try
        {
            repo = (Repository) dataManager.getArtifactStore( event.getStorageItem()
                                                                   .getStoreKey() );
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve store for: %s. Reason: %s", e, event.getStorageItem()
                                                                                  .getStoreKey(), e.getMessage() );
        }

        if ( repo == null )
        {
            return;
        }

        final String path = event.getStorageItem()
                                 .getPath();

        long timeout = config.getPassthroughTimeoutSeconds() * 1000;
        if ( !repo.isPassthrough() )
        {
            timeout = repo.getCacheTimeoutSeconds() * 1000;
        }

        if ( timeout > 0 )
        {
            logger.info( "[PROXY TIMEOUT SET] %s/%s; %s", repo.getKey(), path, new Date( System.currentTimeMillis()
                + timeout ) );
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

    private void setSnapshotTimeouts( final FileEvent event )
    {
        DeployPoint deploy = null;
        try
        {
            final ArtifactStore store = dataManager.getArtifactStore( event.getStorageItem()
                                                                           .getStoreKey() );
            if ( store instanceof DeployPoint )
            {
                deploy = (DeployPoint) store;
            }
            else if ( store instanceof Group )
            {
                final Group group = (Group) store;
                deploy = findDeployPoint( group );
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve deploy point for: %s. Reason: %s", e, event.getStorageItem()
                                                                                         .getStoreKey(), e.getMessage() );
        }

        if ( deploy == null )
        {
            return;
        }

        final String path = event.getStorageItem()
                                 .getPath();

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

    private DeployPoint findDeployPoint( final Group group )
        throws ProxyDataException
    {
        for ( final StoreKey key : group.getConstituents() )
        {
            if ( StoreType.deploy_point == key.getType() )
            {
                return dataManager.getDeployPoint( key.getName() );
            }
            else if ( StoreType.group == key.getType() )
            {
                final Group grp = dataManager.getGroup( key.getName() );
                final DeployPoint dp = findDeployPoint( grp );
                if ( dp != null )
                {
                    return dp;
                }
            }
        }

        return null;
    }

    private void cleanMetadata( final FileEvent event )
    {
        final StoreKey key = event.getStorageItem()
                                  .getStoreKey();
        final String path = event.getStorageItem()
                                 .getPath();

        cleanMetadata( key, path );
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

                    cancel( key, path );

                    final StorageItem item = fileManager.getStorageReference( group, path );
                    if ( item.exists() )
                    {
                        try
                        {
                            item.delete();
                        }
                        catch ( final IOException e )
                        {
                            logger.error( "Failed to delete: %s. Error: %s",
                                          fileManager.getStorageReference( group, path ), e.getMessage() );
                        }
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

    private void cancel( final StoreKey key, final String path )
    {
        final ExpirationKey expirationKey = createAproxFileExpirationKey( key, path );
        try
        {
            expirationManager.cancel( expirationKey );
        }
        catch ( final ExpirationManagerException e )
        {
            logger.error( "Attempting to update groups for metadata change; Failed to expire: %s. Error: %s", e, key,
                          e.getMessage() );
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

    private ExpirationKey createAproxFileExpirationKey( final StoreKey key, final String path )
    {
        final String pathHash = DigestUtils.md5Hex( path );
        return new ExpirationKey( APROX_EVENT, APROX_FILE_EVENT, key.getType()
                                                                    .name(), key.getName(), pathHash );
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
        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );

        final StorageItem item = fileManager.getStorageReference( key, path );
        if ( item.getParent() == null || item.getParent()
                                             .getParent() == null )
        {
            return;
        }

        final StorageItem metadata =
            fileManager.getStorageReference( item.getStoreKey(), item.getParent()
                                                                     .getParent()
                                                                     .getPath(), "maven-metadata.xml" );

        if ( metadata.exists() )
        {
            logger.info( "[UPDATE VERSIONS] Updating snapshot versions for path: %s in store: %s", path, key.getName() );
            Reader reader = null;
            Writer writer = null;
            try
            {
                reader = new InputStreamReader( metadata.openInputStream() );
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

                writer = new OutputStreamWriter( metadata.openOutputStream( true ) );
                new MetadataXpp3Writer().write( writer, md );
            }
            catch ( final IOException e )
            {
                logger.error( "Failed to update metadata after snapshot deletion.\n  Snapshot: %s\n  Metadata: %s\n  Reason: %s",
                              e, item.getFullPath(), metadata, e.getMessage() );
            }
            catch ( final XmlPullParserException e )
            {
                logger.error( "Failed to update metadata after snapshot deletion.\n  Snapshot: %s\n  Metadata: %s\n  Reason: %s",
                              e, item.getFullPath(), metadata, e.getMessage() );
            }
            finally
            {
                closeQuietly( reader );
                closeQuietly( writer );
            }
        }
    }

}
