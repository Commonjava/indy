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
import org.commonjava.indy.core.model.TrackedContentEntry;
import org.commonjava.indy.core.model.dto.ContentTransferDTO;
import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.checksum.ContentDigest;
import org.commonjava.maven.galley.io.checksum.TransferMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.commonjava.indy.core.ctl.PoolUtils.detectOverloadVoid;

@ApplicationScoped
public class TrackingContentController
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentManager contentManager;

    @Inject
    private ContentDigester contentDigester;

    @Inject
    @WeftManaged
    @ExecutorConfig( threads = 50, priority = 4, named = "folo-recalculator", maxLoadFactor = 100, loadSensitive = ExecutorConfig.BooleanLiteral.TRUE )
    private WeftExecutorService recalculationExecutor;

    protected TrackingContentController()
    {
    }

    public TrackingContentController( final ContentManager contentManager, final ContentDigester contentDigester )
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

}
