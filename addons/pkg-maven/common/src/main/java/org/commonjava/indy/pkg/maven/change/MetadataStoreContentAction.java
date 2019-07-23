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
package org.commonjava.indy.pkg.maven.change;

import org.commonjava.indy.content.StoreContentAction;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.pkg.maven.content.MetadataCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Set;

@ApplicationScoped
public class MetadataStoreContentAction
                implements StoreContentAction
{
    private Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private MetadataCacheManager cacheManager;

    public MetadataStoreContentAction()
    {
    }

    @Override
    public void clearStoreContent( String path, ArtifactStore store, Set<Group> affectedGroups,
                                   boolean clearOriginPath )
    {
        logger.debug( "Clearing metadata cache, path: {}, store: {}, affected: {}", path, store.getKey(), affectedGroups );
        cacheManager.remove( store.getKey(), path );
        affectedGroups.forEach( group -> cacheManager.remove( group.getKey(), path ) );
    }
}
