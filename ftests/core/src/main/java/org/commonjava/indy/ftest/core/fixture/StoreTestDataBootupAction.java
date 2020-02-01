/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.ftest.core.fixture;

import org.commonjava.indy.action.BootupAction;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.infinispan.data.StoreDataCache;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static org.commonjava.indy.ftest.core.fixture.StoreTestDataConstants.groupX;
import static org.commonjava.indy.ftest.core.fixture.StoreTestDataConstants.groupY;
import static org.commonjava.indy.ftest.core.fixture.StoreTestDataConstants.repoA;
import static org.commonjava.indy.ftest.core.fixture.StoreTestDataConstants.repoB;
import static org.commonjava.indy.ftest.core.fixture.StoreTestDataConstants.repoC;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;

@ApplicationScoped
public class StoreTestDataBootupAction
        implements BootupAction
{
    @Inject
    @StoreDataCache
    private CacheHandle<StoreKey, ArtifactStore> stores;

    private static boolean enabled = false;

    public static void enable(){
        enabled = true;
    }

    public static void disable(){
        enabled = false;
    }

    final String path = "org/foo/bar/maven-metadata.xml";

    private HostedRepository hostedA = new HostedRepository( PKG_TYPE_MAVEN, repoA );
    private HostedRepository hostedB = new HostedRepository( PKG_TYPE_MAVEN, repoB );
    private HostedRepository hostedC = new HostedRepository( PKG_TYPE_MAVEN, repoC );

    private Group gX = new Group( PKG_TYPE_MAVEN, groupX, hostedA.getKey(), hostedB.getKey() );
    private Group gY = new Group( PKG_TYPE_MAVEN, groupY, gX.getKey() );

    @Override
    public void init()
            throws IndyLifecycleException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "TEST-DATA INIT: initialize store data? {}", enabled );

        if ( enabled )
        {
            stores.put( hostedA.getKey(), hostedA );
            stores.put( hostedB.getKey(), hostedB );
            stores.put( hostedC.getKey(), hostedC );
            stores.put( gX.getKey(), gX );
            stores.put( gY.getKey(), gY );

            int sz = stores.executeCache( Cache::size );
            logger.info( "TEST-DATA INIT: {} test stores added to the cache.", sz );
        }
    }

    @Override
    public int getBootPriority()
    {
        return 10;
    }

    @Override
    public String getId()
    {
        return "Store data test initializer";
    }
}
