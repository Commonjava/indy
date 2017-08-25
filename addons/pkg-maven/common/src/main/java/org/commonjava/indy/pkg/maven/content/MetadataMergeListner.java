/**
 * Copyright (C) 2013 Red Hat, Inc.
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
package org.commonjava.indy.pkg.maven.content;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.maven.inject.MetadataCache;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class MetadataMergeListner
{
    final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    protected DirectContentAccess fileManager;

    @Inject
    @MetadataCache
    private CacheHandle<StoreKey, Map> metadataCache;

    public void onStoreUpdate( @Observes final ArtifactStorePreUpdateEvent event )
    {

        logger.trace( "Got store-update event: {}", event );

        // we're only interested in existing stores, since new stores cannot have indexed keys
        if ( ArtifactStoreUpdateType.UPDATE == event.getType() )
        {
            for ( ArtifactStore store : event )
            {
                removeMetadataCacheContent( store, event.getChangeMap() );
            }
        }
    }

    private void removeMetadataCacheContent( final ArtifactStore store,
                                             final Map<ArtifactStore, ArtifactStore> changeMap )
    {
        StoreKey key = store.getKey();
        // we're only interested in groups, as metadata merging only happens in group level.
        if ( StoreType.group == key.getType() )
        {
            List<StoreKey> newMembers = ( (Group) store ).getConstituents();
            logger.trace( "New members of: {} are: {}", store.getKey(), newMembers );

            Group group = (Group) changeMap.get( store );
            List<StoreKey> oldMembers = group.getConstituents();
            logger.trace( "Old members of: {} are: {}", group.getName(), oldMembers );

            boolean membersChanged = false;

            if ( newMembers.size() != oldMembers.size() )
            {
                membersChanged = true;
            }
            else
            {
                for ( StoreKey storeKey : newMembers )
                {
                    if ( !oldMembers.contains( storeKey ) )
                    {
                        membersChanged = false;
                    }
                }
            }

            if ( membersChanged )
            {
                Map<String, Metadata> metadataMap = metadataCache.get( group.getKey() );

                if ( metadataMap != null && !metadataMap.isEmpty() )
                {
                    metadataMap.keySet().forEach( path -> {
                        try
                        {
                            Transfer tempMetaTxfr = fileManager.getTransfer( group, path );
                            if ( tempMetaTxfr != null && tempMetaTxfr.exists() )
                            {
                                tempMetaTxfr.delete();
                            }
                        }
                        catch ( IndyWorkflowException | IOException e )
                        {
                            logger.error( "Can not delete temp metadata file for group. Group: {}, file path: {}",
                                          group, path );
                        }
                    } );
                }

                metadataCache.remove( store.getKey() );
            }

        }
    }
}
