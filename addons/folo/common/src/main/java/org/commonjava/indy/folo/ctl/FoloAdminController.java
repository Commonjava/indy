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
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.folo.data.FoloContentException;
import org.commonjava.indy.folo.data.FoloFiler;
import org.commonjava.indy.folo.data.FoloRecordCache;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.StoreKey;
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
    private ContentManager contentManager;


    protected FoloAdminController()
    {
    }

    public FoloAdminController( final FoloRecordCache recordManager, final FoloFiler filer,
                                final ContentManager contentManager )
    {
        this.recordManager = recordManager;
        this.filer = filer;
        this.contentManager = contentManager;
    }

    public TrackedContentDTO seal( final String id )
    {
        TrackingKey tk = new TrackingKey( id );
        return constructContentDTO( recordManager.seal( tk ) );
    }

    public File renderRepositoryZip( final String id )
            throws IndyWorkflowException
    {
        final TrackingKey tk = new TrackingKey( id );

        File file = filer.getRepositoryZipFile( tk ).getDetachedFile();
        file.getParentFile().mkdirs();
        logger.debug( "Retrieving tracking record for: {}", tk );
        final TrackedContent record = recordManager.get( tk );
        logger.debug( "Got: {}", record );

        if ( record == null )
        {
            throw new IndyWorkflowException( ApplicationStatus.NOT_FOUND.code(),
                                              "No tracking record available for: %s. Maybe you forgot to seal it?", tk );
        }

        final Set<String> seenPaths = new HashSet<>();
        final List<Transfer> items = new ArrayList<>();

        addTransfers(record.getUploads(), items, id, seenPaths);
        addTransfers(record.getDownloads(), items, id, seenPaths);

        logger.debug( "Retrieved {} files. Creating zip.", items.size() );

        Collections.sort( items, ( f, s ) -> f.getPath().compareTo( s.getPath() ) );


        try (ZipOutputStream stream = new ZipOutputStream( new FileOutputStream( file ) ))
        {
            for ( final Transfer item : items )
            {
                // logger.info( "Adding: {}", item );
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

    private void addTransfers( final Set<TrackedContentEntry> entries, final List<Transfer> items, final String trackingId,
                               final Set<String> seenPaths )
            throws IndyWorkflowException
    {
        if(entries!=null && !entries.isEmpty()){
            for(final TrackedContentEntry entry: entries){
                final String path = entry.getPath();
                if ( path == null || seenPaths.contains( path ) )
                {
                    continue;
                }
                final StoreKey sk = entry.getStoreKey();
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
        final TrackedContentDTO record = constructContentDTO( recordManager.get( tk ) );
        logger.debug( "Got: {}", record );

        if ( record == null )
        {
            throw new IndyWorkflowException( ApplicationStatus.NOT_FOUND.code(),
                                              "No tracking record available for: %s. Maybe you forgot to seal it?", tk );
        }

        final Set<TrackedContentEntryDTO> uploads = record.getUploads();
        final Set<TrackedContentEntryDTO> downloads = record.getDownloads();
        try
        {
            for ( TrackedContentEntryDTO upload : uploads )
            {
                upload.setLocalUrl(
                        UrlUtils.buildUrl( apiBaseUrl, upload.getStoreKey().getType().singularEndpointName(),
                                           upload.getStoreKey().getName(), upload.getPath() ) );
            }
            for ( TrackedContentEntryDTO download : downloads )
            {
                download.setLocalUrl(
                        UrlUtils.buildUrl( apiBaseUrl, download.getStoreKey().getType().singularEndpointName(),
                                           download.getStoreKey().getName(), download.getPath() ) );
            }
        }
        catch ( MalformedURLException e )
        {
            throw new IndyWorkflowException( "Cannot format URL. Reason: %s", e, e.getMessage() );
        }

        return new TrackedContentDTO( tk, uploads, downloads );
    }

    public TrackedContentDTO getRecord( final String id )
            throws IndyWorkflowException
    {
        final TrackingKey tk = new TrackingKey( id );
        return constructContentDTO( recordManager.get( tk ) );
    }

    public void clearRecord( final String id )
            throws FoloContentException
    {
        final TrackingKey tk = new TrackingKey( id );
        recordManager.delete( tk );
    }

    private TrackedContentDTO constructContentDTO( final TrackedContent content )
    {
        if ( content == null )
        {
            return null;
        }
        final Set<TrackedContentEntryDTO> uploads = new TreeSet<>();
        final Set<TrackedContentEntryDTO> downloads = new TreeSet<>();
        content.getUploads().forEach( entry -> uploads.add( constructContentEntryDTO( entry ) ) );
        content.getDownloads().forEach( entry -> downloads.add( constructContentEntryDTO( entry ) ) );
        return new TrackedContentDTO( content.getKey(), uploads, downloads );
    }

    private TrackedContentEntryDTO constructContentEntryDTO( final TrackedContentEntry entry )
    {
        if ( entry == null )
        {
            return null;
        }
        TrackedContentEntryDTO entryDTO = new TrackedContentEntryDTO( entry.getStoreKey(), entry.getAccessChannel(),
                                                                      entry.getPath() );
        entryDTO.setOriginUrl( entry.getOriginUrl() );
        entryDTO.setMd5( entry.getMd5() );
        entryDTO.setSha1( entry.getSha1() );
        entryDTO.setSha256( entry.getSha256() );
        entryDTO.setSize( entry.getSize() );
        return entryDTO;
    }

    public boolean hasRecord( final String id )
    {
        return recordManager.hasRecord( new TrackingKey( id ) );
    }

}
