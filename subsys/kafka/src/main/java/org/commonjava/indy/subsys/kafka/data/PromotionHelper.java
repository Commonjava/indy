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
package org.commonjava.indy.subsys.kafka.data;

import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class PromotionHelper
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public PromotionHelper()
    {
    }

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private NotFoundCache nfc;

    public PromotionHelper( StoreDataManager storeManager, NotFoundCache nfc )
    {
        this.storeManager = storeManager;
        this.nfc = nfc;
    }

    /**
     * Clear NFC for the source store and affected groups.
     *
     * @param sourcePaths The set of paths that need to be cleared from the NFC.
     * @param store The store whose affected groups should have their NFC entries cleared
     */
    public void clearStoreNFC( final Set<String> sourcePaths, final ArtifactStore store,
                               final Set<Group> affectedGroups )
    {
        Set<String> paths = sourcePaths.stream()
                                       .map( sp -> sp.startsWith( "/" ) && sp.length() > 1 ? sp.substring( 1 ) : sp )
                                       .collect( Collectors.toSet() );

        paths.forEach( path -> {
            ConcreteResource resource = new ConcreteResource( LocationUtils.toLocation( store ), path );
            logger.trace( "Clearing NFC path: {} from: {}\n\tResource: {}", path, store.getKey(), resource );
            nfc.clearMissing( resource );
        } );

        Set<Group> groups;

        if ( affectedGroups != null )
        {
            groups = affectedGroups;
        }
        else
        {
            try
            {
                groups = storeManager.query().getGroupsAffectedBy( store.getKey() );
            }
            catch ( IndyDataException e )
            {
                logger.warn( "Failed to clear NFC for groups affected by " + store.getKey(), e );
                return;
            }
        }
        if ( groups != null )
        {
            groups.forEach( group -> {
                KeyedLocation gl = LocationUtils.toLocation( group );
                paths.forEach( path -> {
                    ConcreteResource resource = new ConcreteResource( gl, path );
                    logger.trace( "Clearing NFC path: {} from: {}\n\tResource: {}", path, group.getKey(), resource );
                    nfc.clearMissing( resource );
                } );
            } );
        }
    }
}
