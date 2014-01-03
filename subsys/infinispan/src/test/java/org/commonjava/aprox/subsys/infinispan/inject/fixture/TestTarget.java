/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.subsys.infinispan.inject.fixture;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

@javax.enterprise.context.ApplicationScoped
public class TestTarget
{
    @Inject
    private CacheContainer container;

    @PostConstruct
    public void injectCaches()
    {
        cache = container.getCache( "test" );
        cache.start();

        dataCache = container.getCache( "testData" );
        dataCache.start();
    }

    private Cache<TestKey, TestValue> cache;

    private Cache<String, byte[]> dataCache;

    public Cache<TestKey, TestValue> getCache()
    {
        return cache;
    }

    public Cache<String, byte[]> getDataCache()
    {
        return dataCache;
    }
}
