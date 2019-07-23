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
package org.commonjava.indy.content.index.change;

import org.commonjava.indy.change.event.ArtifactStoreDeletePostEvent;
import org.commonjava.indy.content.index.ContentIndexManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class StoreChangeListener
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentIndexManager contentIndexManager;

    public void storeDeleted( @Observes final ArtifactStoreDeletePostEvent event )
    {
        logger.info( "Updating content index for removed stores." );
        for ( final ArtifactStore store : event )
        {
            logger.info( "Updating content index for removal of: {}", store.getKey() );
            processChanged( store );
        }
    }

    private void processChanged( final ArtifactStore store )
    {
        final StoreKey key = store.getKey();

        logger.trace( "Clean index for: {}", key );
        contentIndexManager.clearAllIndexedPathInStore( store );

        logger.trace( "Clean index with origin: {}", key );
        contentIndexManager.clearAllIndexedPathWithOriginalStore( store );
    }

}
