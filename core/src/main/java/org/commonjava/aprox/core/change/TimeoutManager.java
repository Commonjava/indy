/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.core.change;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.aprox.core.change.sl.ExpirationConstants.APROX_EVENT;
import static org.commonjava.aprox.core.change.sl.ExpirationConstants.APROX_FILE_EVENT;
import static org.commonjava.aprox.util.LocationUtils.getKey;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
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
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.util.ArtifactPathInfo;
import org.commonjava.aprox.util.ArtifactPathInfo.SnapshotInfo;
import org.commonjava.aprox.util.StringFormat;
import org.commonjava.maven.galley.event.FileAccessEvent;
import org.commonjava.maven.galley.event.FileDeletionEvent;
import org.commonjava.maven.galley.event.FileEvent;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.shelflife.ExpirationManager;
import org.commonjava.shelflife.ExpirationManagerException;
import org.commonjava.shelflife.event.ExpirationEvent;
import org.commonjava.shelflife.event.ExpirationEventType;
import org.commonjava.shelflife.match.AndMatcher;
import org.commonjava.shelflife.match.KeyMatcher;
import org.commonjava.shelflife.model.Expiration;
import org.commonjava.shelflife.model.ExpirationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@javax.enterprise.context.ApplicationScoped
public class TimeoutManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

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

                final Transfer toDelete = fileManager.getStorageReference( key, path );
                if ( toDelete.exists() )
                {
                    try
                    {
                        logger.info( "[EXPIRED; DELETE] {}", toDelete );
                        toDelete.delete();
                    }
                    catch ( final IOException e )
                    {
                        logger.error( "{}", e, new StringFormat( "Failed to delete expired file: {}. Reason: {}", toDelete.getFullPath(), e.getMessage() ) );
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
        final TransferOperation type = event.getType();

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
            default:
            {
                break;
            }
        }
    }

    public void onFileAccessEvent( @Observes final FileAccessEvent event )
    {
        final StoreKey key = getKey( event );
        if ( key != null )
        {
            final StoreType type = key.getType();

            if ( type == StoreType.hosted )
            {
                setSnapshotTimeouts( event );
            }
            else if ( type == StoreType.remote )
            {
                setProxyTimeouts( event );
            }
        }
    }

    public void onFileDeletionEvent( @Observes final FileDeletionEvent event )
    {
        final StoreKey key = getKey( event );
        if ( key != null )
        {
            cancel( key, event.getTransfer()
                              .getPath() );
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
                if ( type == StoreType.hosted )
                {
                    //                    logger.info( "[ADJUST TIMEOUTS] Adjusting snapshot expirations in: %s", store.getKey() );
                    rescheduleSnapshotTimeouts( (HostedRepository) store );
                }
                else if ( type == StoreType.remote )
                {
                    //                    logger.info( "[ADJUST TIMEOUTS] Adjusting proxied-file expirations in: %s", store.getKey() );
                    rescheduleProxyTimeouts( (RemoteRepository) store );
                }
            }
        }
    }

    private void rescheduleSnapshotTimeouts( final HostedRepository deploy )
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
                    logger.error( "{}", e, new StringFormat( "Failed to schedule expiration of: {} in {}. Reason: {}", path, deploy.getKey(), e.getMessage() ) );
                    break;
                }
            }
        }
    }

    private void rescheduleProxyTimeouts( final RemoteRepository repo )
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
                    logger.error( "{}", e, new StringFormat( "Failed to schedule expiration of: {} in {}. Reason: {}", path, repo.getKey(), e.getMessage() ) );
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
        final Transfer storeRoot = fileManager.getStoreRootDirectory( store.getKey() );
        final Set<String> paths = new HashSet<String>();
        listAll( storeRoot, "", paths, filter );

        return paths;
    }

    private void listAll( final Transfer dir, final String parentPath, final Set<String> capturedFiles, final FilenameFilter filter )
    {
        final String[] files = dir.exists() ? dir.list() : null;
        if ( files != null )
        {
            for ( final String file : files )
            {
                final File d = dir.getDetachedFile();
                if ( filter == null || filter.accept( d, file ) )
                {
                    final Transfer child = dir.getChild( file );

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
            logger.error( "{}", e, new StringFormat( "Failed to cancel (for purposes of rescheduling) expirations for store: {}. Reason: {}", key, e.getMessage() ) );
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
            final Transfer dir = fileManager.getStoreRootDirectory( key );
            if ( dir.exists() && dir.isDirectory() )
            {
                try
                {
                    logger.info( "[STORE REMOVED; DELETE] {}", dir.getFullPath() );
                    dir.delete();
                    expirationManager.cancelAll( new StoreMatcher( key ) );
                }
                catch ( final IOException e )
                {
                    logger.error( "{}", e, new StringFormat( "Failed to delete storage for deleted artifact store: {} (dir: {}). Error: {}", key, dir, e.getMessage() ) );
                }
                catch ( final ExpirationManagerException e )
                {
                    logger.error( "Failed to cancel file expirations for deleted artifact store: {} (dir: {}). Error: {}", e, key, dir,
                                  e.getMessage() );
                }
            }
        }
    }

    private void setProxyTimeouts( final FileEvent event )
    {
        final StoreKey key = getKey( event );
        if ( key == null )
        {
            return;
        }

        RemoteRepository repo = null;
        try
        {
            repo = (RemoteRepository) dataManager.getArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "{}", e, new StringFormat( "Failed to retrieve store for: {}. Reason: {}", key, e.getMessage() ) );
        }

        if ( repo == null )
        {
            return;
        }

        final String path = event.getTransfer()
                                 .getPath();

        long timeout = config.getPassthroughTimeoutSeconds() * 1000;
        if ( !repo.isPassthrough() )
        {
            timeout = repo.getCacheTimeoutSeconds() * 1000;
        }

        if ( timeout > 0 )
        {
            //            logger.info( "[PROXY TIMEOUT SET] {}/{}; {}", repo.getKey(), path, new Date( System.currentTimeMillis()
            //                + timeout ) );
            try
            {
                final Expiration expiration = createAproxFileExpiration( repo, path, timeout );
                expirationManager.cancelAll( new KeyMatcher( expiration.getKey() ) );

                expirationManager.schedule( expiration );
            }
            catch ( final ExpirationManagerException e )
            {
                logger.error( "Failed to schedule expiration of path: {}\n  Store: {}\n  Timeout: {}\n  Error: {}", e, path, repo, timeout,
                              e.getMessage() );
            }
        }
    }

    private void setSnapshotTimeouts( final FileEvent event )
    {
        final StoreKey key = getKey( event );
        if ( key == null )
        {
            return;
        }

        HostedRepository deploy = null;
        try
        {
            final ArtifactStore store = dataManager.getArtifactStore( key );
            if ( store instanceof HostedRepository )
            {
                deploy = (HostedRepository) store;
            }
            else if ( store instanceof Group )
            {
                final Group group = (Group) store;
                deploy = findDeployPoint( group );
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "{}", e, new StringFormat( "Failed to retrieve deploy point for: {}. Reason: {}", key, e.getMessage() ) );
        }

        if ( deploy == null )
        {
            return;
        }

        final String path = event.getTransfer()
                                 .getPath();

        if ( ArtifactPathInfo.isSnapshot( path ) && deploy.getSnapshotTimeoutSeconds() > 0 )
        {
            final long timeout = deploy.getSnapshotTimeoutSeconds() * 1000;
            //            logger.info( "[SNAPSHOT TIMEOUT SET] {}/{}; {}", deploy.getKey(), path, new Date( timeout ) );
            try
            {
                final Expiration expiration = createAproxFileExpiration( deploy, path, timeout );
                expirationManager.cancelAll( new KeyMatcher( expiration.getKey() ) );

                expirationManager.schedule( expiration );
            }
            catch ( final ExpirationManagerException e )
            {
                logger.error( "Failed to schedule expiration of path: {}\n  Store: {}\n  Timeout: {}\n  Error: {}", e, path, deploy, timeout,
                              e.getMessage() );
            }
        }
    }

    private HostedRepository findDeployPoint( final Group group )
        throws ProxyDataException
    {
        for ( final StoreKey key : group.getConstituents() )
        {
            if ( StoreType.hosted == key.getType() )
            {
                return dataManager.getHostedRepository( key.getName() );
            }
            else if ( StoreType.group == key.getType() )
            {
                final Group grp = dataManager.getGroup( key.getName() );
                final HostedRepository dp = findDeployPoint( grp );
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
        final StoreKey key = getKey( event );
        if ( key == null )
        {
            return;
        }

        final String path = event.getTransfer()
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
                    //                    logger.info( "[CLEAN] Cleaning metadata path: {} in group: {}", path, group.getName() );

                    cancel( key, path );

                    final Transfer item = fileManager.getStorageReference( group, path );
                    if ( item.exists() )
                    {
                        try
                        {
                            item.delete();
                        }
                        catch ( final IOException e )
                        {
                            logger.error( "Failed to delete: {}. Error: {}", fileManager.getStorageReference( group, path ), e.getMessage() );
                        }
                    }
                }
            }
            catch ( final ProxyDataException e )
            {
                logger.error( "Attempting to update groups for metadata change; Failed to retrieve groups containing store: {}. Error: {}", e, key,
                              e.getMessage() );
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
            logger.error( "{}", e, new StringFormat( "Attempting to update groups for metadata change; Failed to expire: {}. Error: {}", key, e.getMessage() ) );
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
                                                                                                    .getName(), pathHash ),
                               System.currentTimeMillis() + timeout, path );
    }

    private boolean isAproxFileExpirationEvent( final ExpirationEvent event )
    {
        return new StoreMatcher().matches( event.getExpiration() );
    }

    private void updateSnapshotVersions( final StoreKey key, final String path )
    {
        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );

        final Transfer item = fileManager.getStorageReference( key, path );
        if ( item.getParent() == null || item.getParent()
                                             .getParent() == null )
        {
            return;
        }

        final Transfer metadata = fileManager.getStorageReference( key, item.getParent()
                                                                            .getParent()
                                                                            .getPath(), "maven-metadata.xml" );

        if ( metadata.exists() )
        {
            //            logger.info( "[UPDATE VERSIONS] Updating snapshot versions for path: {} in store: {}", path, key.getName() );
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

                writer = new OutputStreamWriter( metadata.openOutputStream( TransferOperation.GENERATE, true ) );
                new MetadataXpp3Writer().write( writer, md );
            }
            catch ( final IOException e )
            {
                logger.error( "Failed to update metadata after snapshot deletion.\n  Snapshot: {}\n  Metadata: {}\n  Reason: {}", e,
                              item.getFullPath(), metadata, e.getMessage() );
            }
            catch ( final XmlPullParserException e )
            {
                logger.error( "Failed to update metadata after snapshot deletion.\n  Snapshot: {}\n  Metadata: {}\n  Reason: {}", e,
                              item.getFullPath(), metadata, e.getMessage() );
            }
            finally
            {
                closeQuietly( reader );
                closeQuietly( writer );
            }
        }
    }

}
