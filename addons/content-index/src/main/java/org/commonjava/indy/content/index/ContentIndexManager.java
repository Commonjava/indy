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
package org.commonjava.indy.content.index;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.model.Transfer;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

public interface ContentIndexManager
{
    boolean removeIndexedStorePath( String path, StoreKey key, Consumer<IndexedStorePath> pathConsumer );

    void deIndexStorePath( final StoreKey key, final String path );

    StoreKey getIndexedStoreKey( final StoreKey key, final String path );

    void indexTransferIn( Transfer transfer, StoreKey... topKeys );

    /**
     * When we store or retrieve content, index it for faster reference next time.
     */
    void indexPathInStores( String path, StoreKey originKey, StoreKey... topKeys );

    void clearAllIndexedPathInStore( ArtifactStore store );

    void clearAllIndexedPathWithOriginalStore( ArtifactStore originalStore );

    void clearAllIndexedPathInStoreWithOriginal(ArtifactStore store, ArtifactStore originalStore);

    /**
     * <b>NOT Recursive</b>. This assumes you've recursed the group membership structure beforehand, using
     * {@link org.commonjava.indy.data.ArtifactStoreQuery#getGroupsAffectedBy(Collection)} to find the set of {@link Group} instances for which
     * the path should be cleared.
     */
    void clearIndexedPathFrom( String path, Set<Group> groups, Consumer<IndexedStorePath> pathConsumer );

    String getStrategyPath( final StoreKey key, final String rawPath );
}
