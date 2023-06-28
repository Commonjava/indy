/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.bind.jaxrs.util;

import org.commonjava.cdi.util.weft.DrainingExecutorCompletionService;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.SingleThreadedExecutorService;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentDigester;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.core.model.StoreEffect;
import org.commonjava.indy.core.model.dto.ContentDTO;
import org.commonjava.indy.core.model.TrackedContentEntry;
import org.commonjava.indy.core.model.dto.ContentEntryDTO;
import org.commonjava.indy.core.model.TrackingKey;
import org.commonjava.indy.core.model.dto.ContentTransferDTO;
import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.checksum.ContentDigest;
import org.commonjava.maven.galley.io.checksum.TransferMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.commonjava.indy.core.ctl.PoolUtils.detectOverloadVoid;

@ApplicationScoped
public class ContentAdminController
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentManager contentManager;

    @Inject
    private ContentDigester contentDigester;

    @Inject
    private FoloFiler filer;

    @Inject
    @WeftManaged
    @ExecutorConfig( threads = 50, priority = 4, named = "folo-recalculator", maxLoadFactor = 100, loadSensitive = ExecutorConfig.BooleanLiteral.TRUE )
    private WeftExecutorService recalculationExecutor;

    protected ContentAdminController()
    {
    }

    public ContentAdminController( final ContentManager contentManager, final ContentDigester contentDigester )
    {
        this.contentManager = contentManager;
        this.contentDigester = contentDigester;
        this.recalculationExecutor = new SingleThreadedExecutorService( "folo-recalculator" );
    }

    public Set<TrackedContentEntry> recalculateEntrySet( final Set<ContentTransferDTO> entries, final String id,
                                                         final AtomicBoolean failed ) throws IndyWorkflowException
    {
        if ( entries == null )
        {
            return null;
        }

        DrainingExecutorCompletionService<TrackedContentEntry> recalculateService =
                        new DrainingExecutorCompletionService<>( recalculationExecutor );

        detectOverloadVoid( () -> entries.forEach( entry -> recalculateService.submit( () -> {
            try
            {
                return recalculate( entry );
            }
            catch ( IndyWorkflowException e )
            {
                logger.error( String.format( "Tracking record: %s : Failed to recalculate: %s/%s (%s). Reason: %s", id,
                                             entry.getStoreKey(), entry.getPath(), entry.getEffect(), e.getMessage() ),
                              e );

                failed.set( true );
            }
            return null;
        } ) ) );

        Set<TrackedContentEntry> result = new HashSet<>();
        try
        {
            recalculateService.drain( entry -> {
                if ( entry != null )
                {
                    result.add( entry );
                }
            } );
        }
        catch ( InterruptedException | ExecutionException e )
        {
            logger.error( "Failed to recalculate metadata for Folo tracked content entries in: " + id, e );
            failed.set( true );
        }

        return result;
    }

    private TrackedContentEntry recalculate( final ContentTransferDTO entry ) throws IndyWorkflowException
    {
        StoreKey affectedStore = entry.getStoreKey();
        String path = entry.getPath();
        AccessChannel channel = entry.getAccessChannel();

        Transfer transfer = contentManager.getTransfer( affectedStore, path, entry.getEffect() == StoreEffect.UPLOAD ?
                        TransferOperation.UPLOAD :
                        TransferOperation.DOWNLOAD );

        if ( transfer == null )
        {
            return new TrackedContentEntry( entry.getTrackingKey(), null, null, entry.getOriginUrl(), null,
                                            entry.getEffect(), 0L, "", "", "" );
        }

        contentDigester.removeMetadata( transfer );

        TransferMetadata artifactData = contentDigester.digest( affectedStore, path,
                                                                new EventMetadata( affectedStore.getPackageType() ) );

        Map<ContentDigest, String> digests = artifactData.getDigests();
        return new TrackedContentEntry( entry.getTrackingKey(), affectedStore, channel, entry.getOriginUrl(), path,
                                        entry.getEffect(), artifactData.getSize(), digests.get( ContentDigest.MD5 ),
                                        digests.get( ContentDigest.SHA_1 ), digests.get( ContentDigest.SHA_256 ) );
    }

    public File renderRepositoryZip( final ContentDTO record ) throws IndyWorkflowException
    {
        String id = record.getKey().getId();
        final TrackingKey tk = new TrackingKey( id );

        File file = filer.getRepositoryZipFile( tk ).getDetachedFile();
        file.getParentFile().mkdirs();
        logger.debug( "Got: {}", record );

        if ( record == null )
        {
            throw new IndyWorkflowException( ApplicationStatus.NOT_FOUND.code(),
                                             "No tracking record available for: %s. Maybe you forgot to seal it?", tk );
        }

        final Set<String> seenPaths = new HashSet<>();
        final List<Transfer> items = new ArrayList<>();

        addTransfers( record.getUploads(), items, id, seenPaths );
        addTransfers( record.getDownloads(), items, id, seenPaths );

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

    private void addTransfers( final Set<ContentEntryDTO> entries, final List<Transfer> items,
                               final String trackingId, final Set<String> seenPaths ) throws IndyWorkflowException
    {
        if ( entries != null && !entries.isEmpty() )
        {
            for ( final ContentEntryDTO entry : entries )
            {
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

}
