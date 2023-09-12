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
package org.commonjava.indy.cassandra.data;

import org.commonjava.indy.core.data.GroupDataManagerTCK;
import org.commonjava.indy.core.data.TCKFixtureProvider;
import org.commonjava.indy.cassandra.testcat.CassandraTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.experimental.categories.Category;

@Ignore("Cassandra dbunit always has problems to clean up resources when running test suite in maven")
@Category( CassandraTest.class )
public class CassandraGroupManagementTest
        extends GroupDataManagerTCK
{
    private static CassandraTCKFixtureProvider provider;

    @BeforeClass
    public static void initAll() throws Exception{
        provider = new CassandraTCKFixtureProvider();
        provider.init();
    }

    @Override
    public void doSetup()
            throws Exception
    {

    }

    @Override
    protected TCKFixtureProvider getFixtureProvider()
    {
        return provider;
    }

    @After
    public void tearDown()
    {
        provider.clean();
    }

    @AfterClass
    public static void destroyAll(){
        provider.destroy();
    }
}
