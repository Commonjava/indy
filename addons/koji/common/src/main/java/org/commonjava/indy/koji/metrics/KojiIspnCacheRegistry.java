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
package org.commonjava.indy.koji.metrics;

import org.commonjava.indy.koji.content.IndyKojiContentProvider;
import org.commonjava.indy.subsys.infinispan.metrics.IspnCacheRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Set;

@ApplicationScoped
public class KojiIspnCacheRegistry implements IspnCacheRegistry
{
    private Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyKojiContentProvider kojiContentProvider;

    @Override
    public Set<String> getCacheNames()
    {
        Set<String> ret = kojiContentProvider.getCacheNames();
        logger.info( "[Metrics] Register Koji query cache, names: {}", ret );
        return ret;
    }
}
