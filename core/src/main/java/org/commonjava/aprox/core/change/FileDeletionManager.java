package org.commonjava.aprox.core.change;

import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
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
import org.commonjava.aprox.core.change.event.FileStorageEvent;
import org.commonjava.aprox.core.change.event.FileStorageEvent.Type;
import org.commonjava.aprox.core.change.event.ProxyManagerDeleteEvent;
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
import org.commonjava.shelflife.expire.match.PrefixMatcher;
import org.commonjava.shelflife.model.Expiration;
import org.commonjava.shelflife.model.ExpirationKey;
import org.commonjava.util.logging.Logger;

@Singleton
public class FileDeletionManager
{

    private final Logger logger = new Logger( getClass() );

    public static final String APROX_EVENT = "aprox";

    public static final String APROX_FILE_EVENT = "file";

    private static final PrefixMatcher APROX_FILE_EXPIRATION_MATCHER =
        new PrefixMatcher( APROX_EVENT, APROX_FILE_EVENT );

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

    public void onStoreDeletion( @Observes final ProxyManagerDeleteEvent event )
    {
        final StoreType type = event.getType();
        final Collection<String> names = event.getNames();

        for ( final String name : names )
        {
            final StoreKey key = new StoreKey( type, name );
            final File dir = pathRetriever.getStoreRootDirectory( key );
            try
            {
                logger.info( "[STORE REMOVED; DELETE] %s", dir );
                forceDelete( dir );
                expirationManager.cancelAll( new PrefixMatcher( APROX_EVENT, APROX_FILE_EVENT, type.name(), name ) );
            }
            catch ( final IOException e )
            {
                logger.error( "Failed to delete storage for deleted artifact store: %s (dir: %s). Error: %s", e, key,
                              dir, e.getMessage() );
            }
            catch ( final ExpirationManagerException e )
            {
                logger.error( "Failed to cancel file expirations for deleted artifact store: %s (dir: %s). Error: %s",
                              e, key, dir, e.getMessage() );
            }
        }
    }

    private void setProxyTimeouts( final FileStorageEvent event )
    {
        final Repository repo = (Repository) event.getStore();
        final String path = event.getPath();

        // FIXME: Configurable timeout for "non-cached" files.
        //
        // Even non-cached files need to be cached for a short period, to avoid thrashing connections to the remote
        // proxy target.
        //
        // Therefore, non-cached really means cached with a very short timeout.
        long timeout = 5000;
        if ( repo.isCached() )
        {
            timeout = repo.getCacheTimeoutSeconds() * 1000;
        }

        if ( timeout > 0 )
        {
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

    private void cleanMetadata( final FileStorageEvent event )
    {
        final ArtifactStore store = event.getStore();
        final String path = event.getStorageLocation();

        if ( path.endsWith( "maven-metadata.xml" ) )
        {
            try
            {
                final Set<Group> groups = dataManager.getGroupsContaining( store.getKey() );
                for ( final Group group : groups )
                {
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
                              e, store.getKey(), e.getMessage() );
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
        return APROX_FILE_EXPIRATION_MATCHER.matches( event.getExpiration() );
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
