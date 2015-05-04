/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.mem.data;

import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.commonjava.aprox.core.data.testutil.StoreEventDispatcherStub;
import org.commonjava.aprox.data.StoreDataManager;

public class MemoryTCKFixtureProvider
    implements TCKFixtureProvider
{

    private final MemoryStoreDataManager dataManager = new MemoryStoreDataManager( new StoreEventDispatcherStub() );

    @Override
    public StoreDataManager getDataManager()
    {
        return dataManager;
    }

}
