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
package org.commonjava.indy.folo.ctl;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentDigest;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.folo.data.FoloContentException;
import org.commonjava.indy.folo.data.FoloFiler;
import org.commonjava.indy.folo.data.FoloRecordCache;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.commonjava.indy.folo.model.AffectedStoreRecord;
import org.commonjava.indy.folo.model.TrackedContentRecord;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;

@ApplicationScoped
public class FoloAdminController
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private FoloRecordCache recordManager;

    @Inject
    private FoloFiler filer;

    @Inject
    private DownloadManager downloadManager;

    @Inject
    private ContentManager contentManager;

    @Inject
    private StoreDataManager storeManager;

    protected FoloAdminController()
    {
    }

    public FoloAdminController( final FoloRecordCache recordManager, final FoloFiler filer,
                                final DownloadManager downloadManager, ContentManager contentManager,
                                StoreDataManager storeManager )
    {
        this.recordManager = recordManager;
        this.filer = filer;
        this.downloadManager = downloadManager;
        this.contentManager = contentManager;
        this.storeManager = storeManager;
    }

    public File renderRepositoryZip( final String id )
            throws IndyWorkflowException
    {
        final TrackingKey tk = new TrackingKey( id );

        File file = filer.getRepositoryZipFile( tk ).getDetachedFile();
        file.getParentFile().mkdirs();
        logger.debug( "Retrieving tracking record for: {}", tk );
        final TrackedContentRecord record = recordManager.getIfExists( tk );
        logger.debug( "Got: {}", record );

        if ( record == null )
        {
            throw new IndyWorkflowException( ApplicationStatus.NOT_FOUND.code(),
                                              "No tracking record available for: %s", tk );
        }

        final Set<String> seenPaths = new HashSet<>();
        final List<Transfer> items = new ArrayList<>();
        for ( final AffectedStoreRecord asr : record )
        {
            final StoreKey sk = asr.getKey();

            addTransfers( asr.getUploadedPaths(), sk, items, id, seenPaths );
            addTransfers( asr.getDownloadedPaths(), sk, items, id, seenPaths );
        }

        logger.debug( "Retrieved {} files. Creating zip.", items.size() );

        Collections.sort( items, ( f, s ) -> f.getPath().compareTo( s.getPath() ) );

        try (ZipOutputStream stream = new ZipOutputStream( new FileOutputStream( file ) ))
        {
            for ( final Transfer item : items )
            {
                //                    logger.info( "Adding: {}", item );
                if ( item != null )
                {
                    final String path = item.getPath();
                    final ZipEntry ze = new ZipEntry( path );
                    stream.putNextEntry( ze );

                    InputStream itemStream = null;
                    try
                    {
                        itemStream = item.openInputStream();
                        copy( itemStream, stream );
                    }
                    finally
                    {
                        closeQuietly( itemStream );
                    }
                }
            }
        }
        catch ( final IOException e )
        {
            throw new IndyWorkflowException( "Failed to generate repository zip from tracking record: {}. Reason: {}",
                                              e, id, e.getMessage() );
        }

        return file;
    }

    private void addTransfers( Set<String> paths, StoreKey sk, List<Transfer> items, String trackingId,
                               Set<String> seenPaths )
            throws IndyWorkflowException
    {
        if ( paths != null )
        {
            for ( String path : paths )
            {
                if ( path == null || seenPaths.contains( path ) )
                {
                    continue;
                }

                Transfer transfer = contentManager.getTransfer( sk, path, TransferOperation.DOWNLOAD );
                if ( transfer == null )
                {
                    Logger logger = LoggerFactory.getLogger( getClass() );
                    logger.warn( "While creating Folo repo zip for: {}, cannot find: {} in: {}", trackingId, path, sk );
                }
                else
                {
                    seenPaths.add( path );
                    items.add( transfer );
                }
            }
        }
    }

    public TrackedContentDTO renderReport( final String id, final String apiBaseUrl )
            throws IndyWorkflowException
    {
        final TrackingKey tk = new TrackingKey( id );
        logger.debug( "Retrieving tracking record for: {}", tk );
        final TrackedContentRecord record = recordManager.getIfExists( tk );
        logger.debug( "Got: {}", record );

        if ( record == null )
        {
            throw new IndyWorkflowException( ApplicationStatus.NOT_FOUND.code(),
                                              "No tracking record available for: %s", tk );
        }

        final Set<TrackedContentEntryDTO> uploads = new TreeSet<>();
        final Set<TrackedContentEntryDTO> downloads = new TreeSet<>();
        for ( final AffectedStoreRecord asr : record )
        {
            final StoreKey sk = asr.getKey();

            Set<String> paths = asr.getUploadedPaths();
            if ( paths != null )
            {
                addEntries( uploads, sk, paths, apiBaseUrl );
            }

            paths = asr.getDownloadedPaths();
            if ( paths != null )
            {
                addEntries( downloads, sk, paths, apiBaseUrl );
            }
        }

        return new TrackedContentDTO( tk, uploads, downloads );
    }

    private void addEntries( final Set<TrackedContentEntryDTO> entries, final StoreKey key, final Set<String> paths,
                             final String apiBaseUrl )
            throws IndyWorkflowException
    {
        for ( final String path : paths )
        {
            final Transfer txfr = downloadManager.getStorageReference( key, path );
            if ( txfr != null )
            {
                final TrackedContentEntryDTO entry = new TrackedContentEntryDTO( key, path );

                try
                {
                    final String localUrl =
                            UrlUtils.buildUrl( apiBaseUrl, key.getType().singularEndpointName(), key.getName(), path );

                    String remoteUrl = null;
                    if ( StoreType.remote == key.getType() )
                    {
                        final RemoteRepository repo = storeManager.getRemoteRepository( key.getName() );
                        if ( repo != null )
                        {
                            remoteUrl = UrlUtils.buildUrl( repo.getUrl(), path );
                        }
                    }

                    entry.setLocalUrl( localUrl );
                    entry.setOriginUrl( remoteUrl );

                    final Map<ContentDigest, String> digests =
                            contentManager.digest( key, path, ContentDigest.MD5, ContentDigest.SHA_256 );

                    entry.setMd5( digests.get( ContentDigest.MD5 ) );
                    entry.setSha256( digests.get( ContentDigest.SHA_256 ) );

                    entries.add( entry );
                }
                catch ( final IndyDataException e )
                {
                    throw new IndyWorkflowException(
                            "Cannot retrieve RemoteRepository: %s to calculate remote URL for: %s. Reason: %s", e, key,
                            path, e.getMessage() );
                }
                catch ( final MalformedURLException e )
                {
                    throw new IndyWorkflowException( "Cannot format URL. Reason: %s", e, e.getMessage() );
                }
            }
        }
    }

    public TrackedContentRecord getRecord( final String id )
            throws IndyWorkflowException
    {
        final TrackingKey tk = new TrackingKey( id );
        return recordManager.getIfExists( tk );
    }

    public void clearRecord( final String id )
    {
        final TrackingKey tk = new TrackingKey( id );
        recordManager.delete( tk );
    }

    public boolean hasRecord( final String id )
    {
        return recordManager.hasRecord( new TrackingKey( id ) );
    }

    public void initRecord( final String id )
            throws IndyWorkflowException
    {
        try
        {
            recordManager.getOrCreate( new TrackingKey( id ) );
        }
        catch ( final FoloContentException e )
        {
            throw new IndyWorkflowException( "Failed to initialize tracking record for: %s. Reason: %s", e, id,
                                              e.getMessage() );
        }
    }

}
