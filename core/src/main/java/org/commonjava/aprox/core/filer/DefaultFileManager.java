/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.core.filer;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.change.event.AproxFileEventManager;
import org.commonjava.aprox.change.event.ArtifactStoreRescanEvent;
import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ArtifactPathInfo;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.event.FileAccessEvent;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
public class DefaultFileManager
    implements FileManager
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private AproxConfiguration config;

    @Inject
    private Event<ArtifactStoreRescanEvent> rescanEvent;

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private AproxFileEventManager fileEventManager;

    //    @Inject
    //    private NotFoundCache nfc;

    // Byte, because it's small, and we really only care about the keys anyway.
    private final Map<StoreKey, Byte> rescansInProgress = new ConcurrentHashMap<StoreKey, Byte>();

    @Inject
    @ExecutorConfig( priority = 10, threads = 2, named = "file-manager" )
    private ExecutorService executor; // = Executors.newFixedThreadPool( 8 );

    @Inject
    private TransferManager transfers;

    public DefaultFileManager()
    {
    }

    public DefaultFileManager( final AproxConfiguration config, final TransferManager transfers/*,
                                                                                               final NotFoundCache nfc*/)
    {
        this.config = config;
        this.transfers = transfers/*,
                                       final NotFoundCache nfc*/;
        //        this.nfc = nfc;
        this.fileEventManager = new AproxFileEventManager();
        executor = Executors.newFixedThreadPool( 10 );
    }

    @Override
    public Transfer retrieveFirst( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        try
        {
            return transfers.retrieveFirst( LocationUtils.toLocations( stores ), path );
        }
        catch ( final TransferException e )
        {
            logger.error( e.getMessage(), e );
            throw new AproxWorkflowException( Response.serverError()
                                                      .build() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#downloadAll(java.util.List, java.lang.String)
     */
    @Override
    public Set<Transfer> retrieveAll( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        try
        {
            return transfers.retrieveAll( LocationUtils.toLocations( stores ), path );
        }
        catch ( final TransferException e )
        {
            logger.error( e.getMessage(), e );
            throw new AproxWorkflowException( Response.serverError()
                                                      .build() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#download(org.commonjava.aprox.core.model.ArtifactStore,
     * java.lang.String)
     */
    @Override
    public Transfer retrieve( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        return retrieve( store, path, false );
    }

    private Transfer retrieve( final ArtifactStore store, final String path, final boolean suppressFailures )
        throws AproxWorkflowException
    {
        Transfer target = null;
        try
        {
            if ( store instanceof Repository )
            {
                target = transfers.retrieve( LocationUtils.toLocation( (Repository) store ), path );
            }
            else
            {
                target = transfers.getCacheReference( LocationUtils.toLocation( (DeployPoint) store ), path );
            }

            if ( target.exists() )
            {
                //                logger.info( "Using stored copy from artifact store: %s for: %s", store.getName(), path );
                final Transfer item = getStorageReference( store.getKey(), path );

                return item;
            }
            else
            {
                return null;
            }
        }
        catch ( final TransferException e )
        {
            logger.error( e.getMessage(), e );
            throw new AproxWorkflowException( Response.serverError()
                                                      .build() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#upload(org.commonjava.aprox.core.model.DeployPoint,
     * java.lang.String, java.io.InputStream)
     */
    @Override
    public Transfer store( final DeployPoint deploy, final String path, final InputStream stream )
        throws AproxWorkflowException
    {
        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        if ( pathInfo != null && pathInfo.isSnapshot() )
        {
            if ( !deploy.isAllowSnapshots() )
            {
                logger.error( "Cannot store snapshot in non-snapshot deploy point: %s", deploy.getName() );
                throw new AproxWorkflowException( Response.status( Status.BAD_REQUEST )
                                                          .build() );
            }
        }
        else if ( !deploy.isAllowReleases() )
        {
            logger.error( "Cannot store release in snapshot-only deploy point: %s", deploy.getName() );
            throw new AproxWorkflowException( Response.status( Status.BAD_REQUEST )
                                                      .build() );
        }

        final Transfer target = getStorageReference( deploy, path );

        // TODO: Need some protection for released files!
        // if ( target.exists() )
        // {
        // throw new WebApplicationException(
        // Response.status( Status.BAD_REQUEST ).entity( "Deployment path already exists." ).build() );
        // }

        OutputStream out = null;
        try
        {
            out = target.openOutputStream( TransferOperation.UPLOAD, false );
            copy( stream, out );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to store: %s in deploy store: %s. Reason: %s", e, path, deploy.getName(),
                          e.getMessage() );

            throw new AproxWorkflowException( Response.serverError()
                                                      .build() );
        }
        finally
        {
            closeQuietly( out );
        }

        return target;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#upload(java.util.List, java.lang.String,
     * java.io.InputStream)
     */
    @Override
    public Transfer store( final List<? extends ArtifactStore> stores, final String path, final InputStream stream )
        throws AproxWorkflowException
    {
        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );

        DeployPoint selected = null;
        for ( final ArtifactStore store : stores )
        {
            if ( store instanceof DeployPoint )
            {
                //                logger.info( "Found deploy point: %s", store.getName() );
                final DeployPoint dp = (DeployPoint) store;
                if ( pathInfo == null )
                {
                    // probably not an artifact, most likely metadata instead...
                    //                    logger.info( "Selecting it for non-artifact storage: %s", path );
                    selected = dp;
                    break;
                }
                else if ( pathInfo.isSnapshot() )
                {
                    if ( dp.isAllowSnapshots() )
                    {
                        //                        logger.info( "Selecting it for snapshot storage: %s", pathInfo );
                        selected = dp;
                        break;
                    }
                }
                else if ( dp.isAllowReleases() )
                {
                    //                    logger.info( "Selecting it for release storage: %s", pathInfo );
                    selected = dp;
                    break;
                }
            }
        }

        if ( selected == null )
        {
            logger.warn( "Cannot deploy. No valid deploy points in group." );
            throw new AproxWorkflowException( Response.status( Status.BAD_REQUEST )
                                                      .entity( "No deployment locations available." )
                                                      .build() );
        }

        store( selected, path, stream );

        return getStorageReference( selected.getKey(), path );
    }

    @Override
    public ArtifactPathInfo parsePathInfo( final String path )
    {
        if ( isEmpty( path ) || path.endsWith( "/" ) )
        {
            return null;
        }

        final String[] parts = path.split( "/" );
        if ( parts.length < 4 )
        {
            return null;
        }

        final String file = parts[parts.length - 1];
        final String version = parts[parts.length - 2];
        final String artifactId = parts[parts.length - 3];
        final StringBuilder groupId = new StringBuilder();
        for ( int i = 0; i < parts.length - 3; i++ )
        {
            if ( groupId.length() > 0 )
            {
                groupId.append( '.' );
            }

            groupId.append( parts[i] );
        }

        return new ArtifactPathInfo( groupId.toString(), artifactId, version, file, path );
    }

    @Override
    public Transfer getStoreRootDirectory( final StoreKey key )
    {
        return transfers.getStoreRootDirectory( LocationUtils.toLocation( key ) );
    }

    @Override
    public Transfer getStorageReference( final ArtifactStore store, final String... path )
    {
        return transfers.getCacheReference( LocationUtils.toLocation( store.getKey() ), path );
    }

    @Override
    public Transfer getStorageReference( final StoreKey key, final String... path )
    {
        return transfers.getCacheReference( LocationUtils.toLocation( key ), path );
    }

    @Override
    public boolean deleteAll( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        boolean result = false;
        for ( final ArtifactStore store : stores )
        {
            result = delete( store, path ) || result;
        }

        return result;
    }

    @Override
    public boolean delete( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        final Transfer item = getStorageReference( store, path == null ? ROOT_PATH : path );
        return doDelete( item );
    }

    private Boolean doDelete( final Transfer item )
        throws AproxWorkflowException
    {
        if ( !item.exists() )
        {
            return false;
        }

        if ( item.isDirectory() )
        {
            final String[] listing = item.list();
            for ( final String sub : listing )
            {
                if ( !doDelete( item.getChild( sub ) ) )
                {
                    return false;
                }
            }
        }
        else
        {
            try
            {
                if ( !item.delete() )
                {
                    throw new AproxWorkflowException( Response.serverError()
                                                              .build(), "Failed to delete: %s.", item );
                }
            }
            catch ( final IOException e )
            {
                throw new AproxWorkflowException( Response.serverError()
                                                          .build(), "Failed to delete stored location: %s. Reason: %s",
                                                  e, item, e.getMessage() );
            }
        }

        return true;
    }

    @Override
    public void rescanAll( final List<? extends ArtifactStore> stores )
        throws AproxWorkflowException
    {
        for ( final ArtifactStore store : stores )
        {
            rescan( store );
        }
    }

    @Override
    public void rescan( final ArtifactStore store )
        throws AproxWorkflowException
    {
        executor.execute( new Rescanner( getStorageReference( store.getKey() ), rescansInProgress, fileEventManager,
                                         rescanEvent ) );
    }

    private static final class Rescanner
        implements Runnable
    {
        private static final Byte IN_PROGRESS_FLAG = (byte) 0x1;

        private final Map<StoreKey, Byte> rescansInProgress;

        private final Transfer start;

        private final Event<ArtifactStoreRescanEvent> rescanEvent;

        private final AproxFileEventManager fileEventManager;

        public Rescanner( final Transfer start, final Map<StoreKey, Byte> rescansInProgress,
                          final AproxFileEventManager fileEventManager,
                          final Event<ArtifactStoreRescanEvent> rescanEvent )
        {
            this.start = start;
            this.rescansInProgress = rescansInProgress;
            this.fileEventManager = fileEventManager;
            this.rescanEvent = rescanEvent;
        }

        @Override
        public void run()
        {
            final KeyedLocation kl = (KeyedLocation) start.getLocation();
            final StoreKey storeKey = kl.getKey();
            synchronized ( rescansInProgress )
            {
                if ( rescansInProgress.containsKey( storeKey ) )
                {
                    return;
                }

                rescansInProgress.put( storeKey, IN_PROGRESS_FLAG );
            }

            try
            {
                if ( rescanEvent != null )
                {
                    rescanEvent.fire( new ArtifactStoreRescanEvent( kl.getKey() ) );
                }

                doRescan( start );
            }
            finally
            {
                synchronized ( rescansInProgress )
                {
                    rescansInProgress.remove( storeKey );
                }
            }
        }

        private void doRescan( final Transfer item )
        {
            if ( !item.exists() )
            {
                return;
            }

            if ( item.isDirectory() )
            {
                final String[] listing = item.list();
                for ( final String sub : listing )
                {
                    doRescan( item.getChild( sub ) );
                }
            }

            fileEventManager.fire( new FileAccessEvent( item ) );
        }

    }

}
