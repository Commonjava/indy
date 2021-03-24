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

import org.commonjava.indy.core.data.GroupDataManagerTCK;
import org.commonjava.indy.core.data.TCKFixtureProvider;
import org.junit.Before;

import java.io.IOException;

public class InfinispanGroupManagementTest
        extends GroupDataManagerTCK
{
    private InfinispanTCKFixtureProvider provider;

    @Override
    public void doSetup() throws IOException
    {
        provider = new InfinispanTCKFixtureProvider();
        provider.init();
    }

    @Override
    protected TCKFixtureProvider getFixtureProvider()
    {
        return provider;
    }
}
