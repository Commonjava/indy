/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.ftest.metrics;

import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.ftest.metrics.client.ZabbixCacheStorageTestClientModule;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created by xiabai on 5/8/17.
 */
public class ZabbixCacheStorageTest
                extends AbstractIndyFunctionalTest
{
//    @Test
    public void putCache()
    {
        try
        {
            client.module( ZabbixCacheStorageTestClientModule.class ).putCache();
            org.junit.Assert.assertFalse( false );
        }
        catch ( Exception e )
        {
            org.junit.Assert.assertFalse( true );
        }
    }

    @Test
    public void getCache()
    {
        try
        {

            client.module( ZabbixCacheStorageTestClientModule.class ).putCache();

            org.junit.Assert.assertEquals( "456", client.module( ZabbixCacheStorageTestClientModule.class )
                                                        .getHostGroupCache() );
            org.junit.Assert.assertEquals( "123", client.module( ZabbixCacheStorageTestClientModule.class )
                                                        .getHostCache() );
            org.junit.Assert.assertEquals( "789", client.module( ZabbixCacheStorageTestClientModule.class )
                                                        .getItemCache() );

        }
        catch ( Exception e )

        {
            org.junit.Assert.assertFalse( true );
        }

    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Arrays.<IndyClientModule>asList( new ZabbixCacheStorageTestClientModule() );
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture ) throws IOException
    {
        writeConfigFile( "conf.d/metrics.conf", "\n" + readTestResource( "default-test-metrics.conf" ) );
        writeConfigFile( "default-metrics.conf", "\n" + readTestResource( "default-test-metrics.conf" ) );
    }
}
