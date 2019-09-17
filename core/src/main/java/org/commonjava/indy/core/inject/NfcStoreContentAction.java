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
package org.commonjava.indy.core.inject;

import org.commonjava.indy.content.StoreContentAction;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Set;

@ApplicationScoped
public class NfcStoreContentAction
                implements StoreContentAction
{
    private Logger logger = LoggerFactory.getLogger( getClass() );

    public NfcStoreContentAction()
    {
    }

    @Inject
    private NotFoundCache nfc;

    /**
     * When a path is removed from a store and affected groups, it might because of membership changes. e.g,
     * Group A contains hosted B, and a NFC entry path/to/something exists. If a hosted C is added, we need to
     * clear the NFC entry because the newly added repo may provide such artifact.
     */
    @Override
    public void clearStoreContent( String path, ArtifactStore store, Set<Group> affectedGroups,
                                   boolean clearOriginPath )
    {
        logger.debug( "Clearing NFC path: {}, store: {}, affected: {}", path, store.getKey(), affectedGroups );
        nfc.clearMissing( new ConcreteResource( LocationUtils.toLocation( store ), path ) );
        affectedGroups.forEach(
                        group -> nfc.clearMissing( new ConcreteResource( LocationUtils.toLocation( group ), path ) ) );
    }
}
