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
import org.commonjava.indy.promote.data.PromotionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
            PathsPromoteCompleteEvent promoteCompleteEvent;
            try
            {
                promoteCompleteEvent = mapper.readValue( value, PathsPromoteCompleteEvent.class );
            }
            catch (JsonProcessingException e)
            {
                logger.error("Failed to parse PathsPromoteCompleteEvent", e);
                return;
            }

            logger.info("Handling promote complete event: {}", promoteCompleteEvent);
            StoreKey storeKey = StoreKey.fromString( promoteCompleteEvent.getTargetStore() );
            ArtifactStore store;
            try
            {
                store = ( (ServiceStoreDataManager) storeDataManager ).getArtifactStore( storeKey, true );
                if ( store == null )
                {
                    logger.error( "Failed to fetch store {}", storeKey );
                    return;
                }
            }
            catch (IndyDataException e)
            {
                logger.error( "Failed to fetch store {}", storeKey, e );
                return;
            }

            // clearStoreNFC, null will force querying the affected groups
            promotionHelper.clearStoreNFC( promoteCompleteEvent.getCompletedPaths(), store, null );
        } );
    }

}
