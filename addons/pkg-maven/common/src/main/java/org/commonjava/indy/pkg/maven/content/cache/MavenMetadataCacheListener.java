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
package org.commonjava.indy.pkg.maven.content.cache;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.pkg.maven.content.MetadataInfo;
import org.commonjava.indy.pkg.maven.content.MetadataKey;
import org.commonjava.maven.galley.model.Transfer;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryExpired;
import org.infinispan.notifications.cachelistener.event.CacheEntryExpiredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;

@ApplicationScoped
@Listener
public class MavenMetadataCacheListener
{

    @Inject
    private DirectContentAccess fileManager;

    @CacheEntryExpired
    public void metadataExpired( CacheEntryExpiredEvent<MetadataKey, MetadataInfo> event )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        MetadataKey key = event.getKey();
        StoreKey storeKey = key.getStoreKey();
        String path = key.getPath();

        try
        {
            Transfer target = fileManager.getTransfer( storeKey, path );

            if ( target != null && target.exists() )
            {
                target.delete();
            }
        }
        catch ( IndyWorkflowException | IOException e )
        {
            logger.warn( "On cache expiration, metadata file deletion failed for: " + path + " in store: " + storeKey, e );
        }
    }

}
