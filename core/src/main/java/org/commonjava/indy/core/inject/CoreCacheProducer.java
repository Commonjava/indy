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

import org.commonjava.indy.subsys.infinispan.BasicCacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.commonjava.maven.galley.io.checksum.TransferMetadata;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * Produces ISPN Cache instances (wrapped in {@link BasicCacheHandle} to help with shutdown blocking) for use in core class
 * implementations.
 */
public class CoreCacheProducer
{

    // for content-metadata we mean checksum, md5, sha1, etc
    private static final String CONTENT_METADATA_NAME = "content-metadata";

    @Inject
    private CacheProducer cacheProducer;

    @ContentMetadataCache
    @Produces
    @ApplicationScoped
    public BasicCacheHandle<String, TransferMetadata> contentMetadataCache()
    {
        return cacheProducer.getBasicCache( CONTENT_METADATA_NAME );
    }
}
