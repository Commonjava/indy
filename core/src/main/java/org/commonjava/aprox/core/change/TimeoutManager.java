/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
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
import java.util.concurrent.Executor;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.change.event.ArtifactStoreDeleteEvent;
import org.commonjava.aprox.change.event.ArtifactStoreUpdateType;
import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.core.change.sl.LoggingMatcher;
import org.commonjava.aprox.core.change.sl.MaxTimeoutMatcher;
import org.commonjava.aprox.core.change.sl.SnapshotFilter;
import org.commonjava.aprox.core.change.sl.StoreMatcher;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;
import org.commonjava.maven.atlas.ident.util.SnapshotUtils;
import org.commonjava.maven.atlas.ident.version.part.SnapshotPart;
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
    private DownloadManager fileManager;

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private AproxConfiguration config;

    @Inject
    @ExecutorConfig( daemon = true, priority = 7, named = "aprox-events" )
    private Executor executor;

    public void onExpirationEvent( @Observes final ExpirationEvent event )
    {
        executor.execute( new Runnable()
        {
            @Override
            public void run()
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

                        Transfer toDelete;
                        try
                        {
                            toDelete = fileManager.getStorageReference( key, path );
                        }
                        catch ( final AproxWorkflowException e )
                        {
                            logger.error( String.format( "Failed to delete expired file for: %s, %s. Reason: %s", key,
                                                         path, e.getMessage() ), e );

                            return;
                        }

                        if ( toDelete.exists() )
                        {
                            try
                            {
                                logger.info( "[EXPIRED; DELETE] {}", toDelete );
                                toDelete.delete();
                            }
                            catch ( final IOException e )
                            {
                                logger.error( String.format( "Failed to delete expired file: %s. Reason: %s",
                                                             toDelete.getFullPath(), e.getMessage() ), e );
                            }

                            cleanMetadata( key, path );
                        }

                        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
                        if ( pathInfo == null )
                        {
                            return;
                        }

                        if ( pathInfo.isSnapshot() )
                        {
                            updateSnapshotVersions( key, path );
                        }
                    }
                }
            }
        } );
    }

    public void onFileStorageEvent( @Observes final FileStorageEvent event )
    {
        executor.execute( new Runnable()
        {
            @Override
            public void run()
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
        } );
    }

    public void onFileAccessEvent( @Observes final FileAccessEvent event )
    {
        executor.execute( new Runnable()
        {
            @Override
            public void run()
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
        } );
    }

    public void onFileDeletionEvent( @Observes final FileDeletionEvent event )
    {
        executor.execute( new Runnable()
        {
            @Override
            public void run()
            {
                final StoreKey key = getKey( event );
                if ( key != null )
                {
                    cancel( key, event.getTransfer()
                                      .getPath() );
                }
            }
        } );
    }

    public void onStoreUpdate( @Observes final ArtifactStoreUpdateEvent event )
    {
        executor.execute( new Runnable()
        {
            @Override
            public void run()
            {
                final ArtifactStoreUpdateType eventType = event.getType();
                if ( eventType == ArtifactStoreUpdateType.ADD_OR_UPDATE )
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
        } );
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
                    logger.error( String.format( "Failed to schedule expiration of: %s in %s. Reason: %s", path,
                                                 deploy.getKey(), e.getMessage() ), e );
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
                    logger.error( String.format( "Failed to schedule expiration of: %s in %s. Reason: %s", path,
                                                 repo.getKey(), e.getMessage() ), e );
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
        final Transfer storeRoot = fileManager.getStoreRootDirectory( store );
        final Set<String> paths = new HashSet<String>();
        listAll( storeRoot, "", paths, filter );

        return paths;
    }

    private void listAll( final Transfer dir, final String parentPath, final Set<String> capturedFiles,
                          final FilenameFilter filter )
    {
        try
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
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to list local contents of: %s. Reason: %s", dir, e.getMessage() ), e );
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
            logger.error( String.format( "Failed to cancel (for purposes of rescheduling) expirations for store: %s. Reason: %s",
                                         key, e.getMessage() ), e );
        }

        return null;
    }

    public void onStoreDeletion( @Observes final ArtifactStoreDeleteEvent event )
    {
        executor.execute( new Runnable()
        {
            @Override
            public void run()
            {
                final StoreType type = event.getType();
                final Collection<String> names = event.getNames();

                for ( final String name : names )
                {
                    final StoreKey key = new StoreKey( type, name );
                    Transfer dir;
                    try
                    {
                        dir = fileManager.getStoreRootDirectory( key );
                    }
                    catch ( final AproxWorkflowException e )
                    {
                        logger.error( "Failed to cancel file expirations for deleted artifact store: {}. Error: {}", e,
                                      key, e.getMessage() );
                        return;
                    }

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
                            logger.error( String.format( "Failed to delete storage for deleted artifact store: %s (dir: %s). Error: %s",
                                                         key, dir, e.getMessage() ), e );
                        }
                        catch ( final ExpirationManagerException e )
                        {
                            logger.error( String.format( "Failed to cancel file expirations for deleted artifact store: {} (dir: {}). Error: {}",
                                                         key, dir, e.getMessage() ), e );
                        }
                    }
                }
            }
        } );
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
            logger.error( String.format( "Failed to retrieve store for: %s. Reason: %s", key, e.getMessage() ), e );
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
                logger.error( "Failed to schedule expiration of path: {}\n  Store: {}\n  Timeout: {}\n  Error: {}", e,
                              path, repo, timeout, e.getMessage() );
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
            if ( store == null )
            {
                return;
            }

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
            logger.error( String.format( "Failed to retrieve deploy point for: %s. Reason: %s", key, e.getMessage() ),
                          e );
        }

        if ( deploy == null )
        {
            return;
        }

        final String path = event.getTransfer()
                                 .getPath();

        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        if ( pathInfo == null )
        {
            return;
        }

        if ( pathInfo.isSnapshot() && deploy.getSnapshotTimeoutSeconds() > 0 )
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
                logger.error( "Failed to schedule expiration of path: {}\n  Store: {}\n  Timeout: {}\n  Error: {}", e,
                              path, deploy, timeout, e.getMessage() );
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
                            logger.error( "Failed to delete: {}. Error: {}",
                                          fileManager.getStorageReference( group, path ), e.getMessage() );
                        }
                    }
                }
            }
            catch ( final ProxyDataException e )
            {
                logger.error( String.format( "Attempting to update groups for metadata change; Failed to retrieve groups containing store: {}. Error: {}",
                                             key, e.getMessage() ), e );
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
            logger.error( String.format( "Attempting to update groups for metadata change; Failed to expire: %s. Error: %s",
                                         key, e.getMessage() ), e );
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
            return new StoreKey( StoreType.get( parts[2] ), parts[3] );
        }
    }

    private ExpirationKey createAproxFileExpirationKey( final StoreKey key, final String path )
    {
        return new ExpirationKey( APROX_EVENT, APROX_FILE_EVENT, key.getType()
                                                                    .name(), key.getName(), path );
    }

    private Expiration createAproxFileExpiration( final ArtifactStore store, final String path, final long timeout )
    {
        return new Expiration( new ExpirationKey( APROX_EVENT, APROX_FILE_EVENT, store.getKey()
                                                                                      .getType()
                                                                                      .name(), store.getKey()
                                                                                                    .getName(), path ),
                               System.currentTimeMillis() + timeout, path );
    }

    private boolean isAproxFileExpirationEvent( final ExpirationEvent event )
    {
        return new StoreMatcher().matches( event.getExpiration() );
    }

    private void updateSnapshotVersions( final StoreKey key, final String path )
    {
        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        if ( pathInfo == null )
        {
            return;
        }

        final ArtifactStore store;
        try
        {
            store = dataManager.getArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            logger.error( String.format( "Failed to update metadata after snapshot deletion. Reason: {}",
                                         e.getMessage() ), e );
            return;
        }

        if ( store == null )
        {
            logger.error( "Failed to update metadata after snapshot deletion in: {}. Reason: Cannot find corresponding ArtifactStore",
                          key );
            return;
        }

        final Transfer item = fileManager.getStorageReference( store, path );
        if ( item.getParent() == null || item.getParent()
                                             .getParent() == null )
        {
            return;
        }

        final Transfer metadata = fileManager.getStorageReference( store, item.getParent()
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

                if ( version.equals( md.getVersion() ) )
                {
                    md.setVersion( replacement );
                }

                if ( version.equals( versioning.getLatest() ) )
                {
                    versioning.setLatest( replacement );
                }

                final SnapshotPart si = pathInfo.getSnapshotInfo();
                if ( si != null )
                {
                    final SnapshotPart siRepl = SnapshotUtils.extractSnapshotVersionPart( replacement );
                    final Snapshot snapshot = versioning.getSnapshot();

                    final String siTstamp = SnapshotUtils.generateSnapshotTimestamp( si.getTimestamp() );
                    if ( si.isRemoteSnapshot() && siTstamp.equals( snapshot.getTimestamp() )
                        && si.getBuildNumber() == snapshot.getBuildNumber() )
                    {
                        if ( siRepl != null )
                        {
                            if ( siRepl.isRemoteSnapshot() )
                            {
                                snapshot.setTimestamp( SnapshotUtils.generateSnapshotTimestamp( siRepl.getTimestamp() ) );
                                snapshot.setBuildNumber( siRepl.getBuildNumber() );
                            }
                            else
                            {
                                snapshot.setLocalCopy( true );
                            }
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
                logger.error( "Failed to update metadata after snapshot deletion.\n  Snapshot: {}\n  Metadata: {}\n  Reason: {}",
                              e, item.getFullPath(), metadata, e.getMessage() );
            }
            catch ( final XmlPullParserException e )
            {
                logger.error( "Failed to update metadata after snapshot deletion.\n  Snapshot: {}\n  Metadata: {}\n  Reason: {}",
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
