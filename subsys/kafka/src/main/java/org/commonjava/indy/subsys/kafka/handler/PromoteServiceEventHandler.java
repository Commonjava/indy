/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.kafka.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.streams.kstream.KStream;
import org.commonjava.event.promote.PathsPromoteCompleteEvent;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.db.service.ServiceStoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.kafka.data.PromotionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.HashSet;
import java.util.Set;

import static org.commonjava.indy.subsys.kafka.event.TopicType.PROMOTE_COMPLETE_EVENT;

@ApplicationScoped
public class PromoteServiceEventHandler
        implements ServiceEventHandler
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ObjectMapper mapper;

    @Inject
    PromotionHelper promotionHelper;

    @Inject
    StoreDataManager storeDataManager;

    @Override
    public boolean canHandle(String topic)
    {
        return topic.equals(PROMOTE_COMPLETE_EVENT.getName());
    }

    @Override
    public void dispatchEvent( KStream<String, String> stream, String topic )
    {
        logger.debug( "Dispatch event for {}", this.getClass() );
        stream.foreach( ( key, value ) -> {
            PathsPromoteCompleteEvent completeEvent;
            try
            {
                completeEvent = mapper.readValue( value, PathsPromoteCompleteEvent.class );
            }
            catch (JsonProcessingException e)
            {
                logger.error("Failed to parse PathsPromoteCompleteEvent", e);
                return;
            }

            logger.info("Handling promote complete event: {}", completeEvent);
            final StoreKey sourceStoreKey = StoreKey.fromString( completeEvent.getSourceStore() );
            final StoreKey targetStoreKey = StoreKey.fromString( completeEvent.getTargetStore() );
            final ArtifactStore targetStore, sourceStore;
            try
            {
                sourceStore = ( (ServiceStoreDataManager) storeDataManager ).getArtifactStore( sourceStoreKey, true );
                targetStore = ( (ServiceStoreDataManager) storeDataManager ).getArtifactStore( targetStoreKey, true );
                if ( sourceStore == null || targetStore == null )
                {
                    logger.error( "Failed to fetch stores, sourceStore: {}, targetStore: {}", sourceStore, targetStore );
                    return;
                }
            }
            catch (IndyDataException e)
            {
                logger.error( "Failed to fetch stores", e );
                return;
            }

            Set<String> clearPaths = new HashSet<>();
            addClearPaths(clearPaths, completeEvent.getCompletedPaths());
            addClearPaths(clearPaths, completeEvent.getSkippedPaths());

            // clear store NFC, null will force querying the affected groups
            promotionHelper.clearStoreNFC( clearPaths, targetStore, null );

            // when purging source, we also clean source affected groups
            if ( completeEvent.isPurgeSource() )
            {
                promotionHelper.clearStoreNFC( clearPaths, sourceStore, null );
            }
        } );
    }

    private void addClearPaths(Set<String> clearPaths, Set<String> paths)
    {
        if ( paths != null )
        {
            clearPaths.addAll(paths);
        }
    }

}
