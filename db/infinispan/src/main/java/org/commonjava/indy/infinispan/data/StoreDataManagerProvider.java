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
package org.commonjava.indy.infinispan.data;

import org.commonjava.indy.cassandra.data.ClusterStoreDataManager;
import org.commonjava.indy.core.conf.IndyDurableStateConfig;
import org.commonjava.indy.data.StandaloneStoreDataManager;
import org.commonjava.indy.data.StoreDataManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class StoreDataManagerProvider
{

    @Inject
    IndyDurableStateConfig durableStateConfig;

    @Produces
    @Default
    public StoreDataManager getStoreDataManager(
                    @StandaloneStoreDataManager InfinispanStoreDataManager ispnStoreDataManager,
                    @ClusterStoreDataManager StoreDataManager clusterStoreDataManager )
    {
        if ( durableStateConfig.getStoreStorage().equals( IndyDurableStateConfig.STORAGE_INFINISPAN ) )
        {
            return ispnStoreDataManager;
        }
        else if ( durableStateConfig.getStoreStorage().equals( IndyDurableStateConfig.STORAGE_CASSANDRA ) )
        {
            return clusterStoreDataManager;
        }
        else
        {
            throw new RuntimeException(
                            "Invalid configuration for store manager:" + durableStateConfig.getStoreStorage() );
        }
    }
}
