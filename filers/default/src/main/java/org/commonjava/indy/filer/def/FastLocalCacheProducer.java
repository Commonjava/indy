/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.filer.def;

import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.commonjava.maven.galley.model.ConcreteResource;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class FastLocalCacheProducer
{
    @Inject
    private CacheProducer cacheProducer;

    @NFSOwnerCache
    @Produces
    @ApplicationScoped
    public CacheHandle<String, String> nfsOwnerCacheCfg()
    {
        return cacheProducer.getCache( "indy-nfs-owner-cache" );
    }

    @FastLocalFileRemoveCache
    @Produces
    @ApplicationScoped
    public CacheHandle<String, ConcreteResource> fastLocalFileRemoveCfg()
    {
        return cacheProducer.getCache( "indy-fastlocal-file-delete-cache" );
    }
}
