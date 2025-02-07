/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.commonjava.maven.galley.spi.proxy.ProxySitesCache;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
@Alternative
public class MemoryProxySitesCache
        implements ProxySitesCache
{

    protected Set<String> proxySitesCache = new HashSet<>();

    @Override
    public Set<String> getProxySites()
    {
        return proxySitesCache;
    }

    @Override
    public boolean isProxySite( String site )
    {
        return proxySitesCache.contains( site );
    }

    @Override
    public void saveProxySite( String site )
    {
        proxySitesCache.add( site );
    }

    @Override
    public void deleteProxySite( String site )
    {
        proxySitesCache.remove( site );
    }

    @Override
    public void deleteAllProxySites()
    {
        proxySitesCache.clear();
    }
}
