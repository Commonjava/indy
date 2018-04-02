/**
 * Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.ftest.core.infinispan;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by yma on 2018/1/25.
 */

/**
 * This test use initTestConfig to assure the customer's ISPN config file,
 * and then verify CacheProducer could handle the xml config files merging stuff
 * from the customers' side and classpath resource (default).
 */
public class CacheProducerMergeXmlTest
        extends AbstractIndyFunctionalTest
{

    private CacheProducer producer;

    private EmbeddedCacheManager manager;

    @Before
    public void setup()
    {
        manager = new DefaultCacheManager(
                new GlobalConfigurationBuilder().globalJmxStatistics().allowDuplicateDomains( true ).build() );
        producer = new CacheProducer( new DefaultIndyConfiguration(), manager );
        producer.start();
        manager = producer.getCacheManager();
    }

    @Test
    public void testMerge()
            throws Exception
    {
        assertThat( manager.getCacheConfiguration( "local" ).eviction().size(), equalTo( 10000000L ) );
        assertThat( manager.getCacheConfiguration( "koji-maven-version-metadata" ).eviction().size(),
                    equalTo( 10000000L ) );
        assertThat( manager.getCacheConfiguration( "folo-in-progress" ).eviction().size(), equalTo( 10000000L ) );
        assertThat( manager.getCacheConfiguration( "folo-sealed" ).persistence().passivation(), equalTo( false ) );
        assertThat( manager.getCacheConfiguration( "content-index" ).eviction().size(), equalTo( 10000000L ) );
        assertThat( manager.getCacheConfiguration( "indy-nfs-owner-cache" ).eviction().size(), equalTo( 10000000L ) );
        assertThat( manager.getCacheConfiguration( "nfc" ), notNullValue() );
        assertThat( manager.getCacheConfiguration( "schedule-expire-cache" ), notNullValue() );
        assertThat( manager.getCacheConfiguration( "maven-version-metadata-cache" ), notNullValue() );
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        writeConfigFile( "infinispan.xml", readTestResource( "infinispan-test.xml" ) );
    }

    @After
    public void shutdown()
            throws IndyLifecycleException

    {
        if ( producer != null )
        {
            producer.stop();
        }
    }
}
