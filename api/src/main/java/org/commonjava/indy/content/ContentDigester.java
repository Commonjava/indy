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
package org.commonjava.indy.content;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.checksum.ContentDigest;
import org.commonjava.maven.galley.io.checksum.TransferMetadata;
import org.commonjava.maven.galley.io.checksum.TransferMetadataConsumer;
import org.commonjava.maven.galley.model.Transfer;

/**
 * Created by jdcasey on 1/4/17.
 * Handles caching of content metadata (size, checksums), and also provides methods for calculating this metadata
 * on demand. Implements {@link TransferMetadataConsumer}, allowing it to passively cache calculated metadata as
 * files are written to storage.
 */
public interface ContentDigester
        extends TransferMetadataConsumer
{

    TransferMetadata getContentMetadata( Transfer transfer );

    TransferMetadata digest( final StoreKey affectedStore, final String s, final EventMetadata eventMetadata )
            throws IndyWorkflowException;
}
