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
package org.commonjava.indy.cassandra.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.data.StoreDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

@Named("Store-Cache-Initialization")
public class StoreDataStartupAction implements StartupAction
{

    @Inject
    StoreDataManager dataManager;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public String getId()
    {
        return "Init remote store cache based on the persistent store data.";
    }

    @Override
    public void start() throws IndyLifecycleException
    {

        if ( dataManager instanceof CassandraStoreDataManager )
        {
            logger.info( "Init the cache of remote stores based on the store data" );

            ( (CassandraStoreDataManager) dataManager ).initRemoteStoresCache();
        }

    }

    @Override
    public int getStartupPriority()
    {
        return 90;
    }
}
