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
package org.commonjava.indy.infinispan.data;

import org.commonjava.indy.core.conf.IndyDurableStateConfig;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.db.common.inject.Clustered;
import org.commonjava.indy.db.common.inject.Serviced;
import org.commonjava.indy.db.common.inject.Standalone;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * @deprecated The store management functions has been extracted into Repository Service, which is maintained in "ServiceStoreDataManager"
 */
@Deprecated
@ApplicationScoped
public class StoreDataManagerProvider
{

    @Inject
    IndyDurableStateConfig durableStateConfig;

    @Produces
    @Default
    public StoreDataManager getStoreDataManager(
            @Standalone StoreDataManager ispnStoreDataManager,
            @Serviced StoreDataManager serviceStoreDataManager )
    {
        if ( IndyDurableStateConfig.STORAGE_INFINISPAN.equals( durableStateConfig.getStoreStorage() ) )
        {
            return ispnStoreDataManager;
        }
        else if ( IndyDurableStateConfig.STORAGE_SERVICE.equals( durableStateConfig.getStoreStorage()) )
        {
            return serviceStoreDataManager;
        }
        else
        {
            throw new RuntimeException(
                            "Invalid configuration for store manager:" + durableStateConfig.getStoreStorage() );
        }
    }
}
