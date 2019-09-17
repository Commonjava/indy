/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentDigester;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.core.inject.ContentMetadataCache;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.subsys.infinispan.BasicCacheHandle;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.checksum.TransferMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import static org.commonjava.maven.galley.io.ChecksummingTransferDecorator.FORCE_CHECKSUM;

/**
 * Created by jdcasey on 1/4/17.
 * Migrated out of DefaultContentManager so it can be used from other places. This isn't really specific to the
 * {@link org.commonjava.indy.content.ContentManager} interface anyway.
 */
@ApplicationScoped
public class DefaultContentDigester
        implements ContentDigester

{

    @Inject
    private DirectContentAccess directContentAccess;

    @Inject
    @ContentMetadataCache
    private BasicCacheHandle<String, TransferMetadata> metadataCache;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected DefaultContentDigester()
    {
    }

    public DefaultContentDigester( DirectContentAccess directContentAccess,
                                   BasicCacheHandle<String, TransferMetadata> metadataCache )
    {
        this.directContentAccess = directContentAccess;
        this.metadataCache = metadataCache;
    }

    @Override
    public void addMetadata( final Transfer transfer, final TransferMetadata transferData )
    {
        if ( transferData != null )
        {
            String cacheKey = generateCacheKey( transfer );
            logger.trace( "Adding TransferMetadata for: {}\n{}", cacheKey, transferData );
            metadataCache.put( cacheKey, transferData );
        }
    }

    public boolean needsMetadataFor( final Transfer transfer )
    {
        return true;
    }

    private String generateCacheKey( final Transfer transfer )
    {
        KeyedLocation kl = (KeyedLocation) transfer.getLocation();
        return kl.getKey() + "#" + transfer.getPath();
    }

    @Override
    public void removeMetadata( final Transfer transfer )
    {
        String cacheKey = generateCacheKey( transfer );
        TransferMetadata meta = metadataCache.remove( cacheKey );
        logger.trace( "Removing TransferMetadata for: {}\n{}", cacheKey, meta );
    }

    @Override
    public TransferMetadata getContentMetadata( final Transfer transfer )
    {
        String cacheKey = generateCacheKey( transfer );
        logger.trace( "Getting TransferMetadata for: {}", cacheKey );

        TransferMetadata metadata = metadataCache.get( cacheKey );

        if ( metadata != null )
        {
            logger.trace( "[CACHE HIT] Returning content metadata for: {}\n\n{}\n\n", cacheKey, metadata );
        }
        else
        {
            logger.trace( "[CACHE MISS] Cannot find content metadata for: {}!", cacheKey );
        }
        return metadata;
    }

    public TransferMetadata digest( final StoreKey key, final String path, final EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        final Transfer transfer = directContentAccess.getTransfer( key, path );
        if ( transfer == null || !transfer.exists() )
        {
            return new TransferMetadata( Collections.emptyMap(), 0L );
        }

        TransferMetadata meta = getContentMetadata( transfer );
        if ( meta != null )
        {
            return meta;
        }

        String cacheKey = generateCacheKey( transfer );
        logger.debug( "TransferMetadata missing for: {}. Re-reading with FORCE_CHECKSUM now to calculate it.",
                      cacheKey );

        EventMetadata forcedEventMetadata = new EventMetadata( eventMetadata ).set( FORCE_CHECKSUM, Boolean.TRUE );
        try(InputStream stream = transfer.openInputStream( false, forcedEventMetadata ) )
        {
            // depend on ChecksummingTransferDecorator to calculate / store metadata as this gets read, using
            // the FORCE_CHECKSUM metadata key to control its generation.
            IOUtils.toByteArray( stream );
        }
        catch ( IOException e )
        {
            throw new IndyWorkflowException( "Failed to calculate checksums (MD5, SHA-256, etc.) for: %s. Reason: %s",
                                             e, transfer, e.getMessage() );
        }

        logger.debug( "Retrying TransferMetadata retrieval from cache for: {} after recalculating", cacheKey );

        return getContentMetadata( transfer );
    }
}
