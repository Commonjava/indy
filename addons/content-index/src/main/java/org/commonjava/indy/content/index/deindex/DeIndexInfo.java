/**
 * Copyright (C) 2013~2019 Red Hat, Inc.
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
package org.commonjava.indy.content.index.deindex;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.model.Transfer;

public class DeIndexInfo
{
    public static final String TYPE_SINGLE = "SINGLE";

    public static final String TYPE_MULTI = "MULTIPLE";

    final Transfer transfer;

    final ArtifactStore store;

    final String deIndexType;

    final StoreKey topKey;

    public DeIndexInfo( final Transfer transfer, final ArtifactStore store, final String deIndexType )
    {
        this( transfer, store, null, deIndexType );
    }

    public DeIndexInfo( final Transfer transfer, final ArtifactStore store, final StoreKey topKey,
                        final String deIndexType )
    {
        this.transfer = transfer;
        this.store = store;
        this.deIndexType = deIndexType;
        this.topKey = topKey;
    }
}
