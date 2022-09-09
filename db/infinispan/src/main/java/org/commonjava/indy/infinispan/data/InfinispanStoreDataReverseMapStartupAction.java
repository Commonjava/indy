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

import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.data.StoreDataManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @deprecated The store management functions has been extracted into Repository Service, which is maintained in "ServiceStoreDataManager"
 */
@Deprecated
@ApplicationScoped
public class InfinispanStoreDataReverseMapStartupAction
        implements StartupAction
{
    @Inject
    private StoreDataManager storeDataManager;

    @Override
    public void start()
    {
        if ( storeDataManager instanceof InfinispanStoreDataManager )
        {
            ( (InfinispanStoreDataManager) storeDataManager ).initAffectedBy();
        }
    }

    @Override
    public int getStartupPriority()
    {
        return 10;
    }

    @Override
    public String getId()
    {
        return "Infinispan affected-by reverse-map initializer";
    }
}
