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
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.infinispan.eviction.EvictionType.COUNT;
import static org.infinispan.eviction.EvictionType.MEMORY;
import static org.junit.Assert.assertThat;

/**
 * This test use initTestConfig to write the customer's ISPN config file,
 * and verify CacheProducer merges the customer config and default config (classpath resource).
 */
public class CacheProducerMergeXmlTest
        extends AbstractIndyFunctionalTest
{

    private CacheProducer producer;

    private EmbeddedCacheManager manager;

    @Before
    public void setup()
    {
        System.setProperty( "GlobalJmxStatistics.allowDuplicateDomains", "true" );

        producer = new CacheProducer( new DefaultIndyConfiguration() );
        producer.start();
        manager = producer.getCacheManager();
    }

    private static final String local = "local";
    private static final String koji_maven_version_metadata = "koji-maven-version-metadata";
    private static final String folo_in_progress = "folo-in-progress";
    private static final String folo_sealed = "folo-sealed";
    private static final String content_metadata = "content-metadata";
    private static final String content_index = "content-index";
    private static final String maven_version_metadata_cache = "maven-version-metadata-cache";
    private static final String schedule_expire_cache = "schedule-expire-cache";
    private static final String nfc = "nfc";

    @Test
    public void testMerge()
            throws Exception
    {
        assertThat( manager.getCacheConfiguration( local ), notNullValue() );
        assertThat( manager.getCacheConfiguration( koji_maven_version_metadata ), notNullValue() );
        assertThat( manager.getCacheConfiguration( folo_in_progress ), notNullValue() );
        assertThat( manager.getCacheConfiguration( folo_sealed ), notNullValue() );
        assertThat( manager.getCacheConfiguration( content_metadata ), notNullValue() );
        assertThat( manager.getCacheConfiguration( content_index ), notNullValue() );
        assertThat( manager.getCacheConfiguration( maven_version_metadata_cache ), notNullValue() );
        assertThat( manager.getCacheConfiguration( schedule_expire_cache ), notNullValue() );
        assertThat( manager.getCacheConfiguration( nfc ), notNullValue() );

        assertThat( manager.getCacheConfiguration( nfc ).memory().size(), equalTo( 20971520L ) );
        assertThat( manager.getCacheConfiguration( nfc ).memory().isEvictionEnabled(), equalTo( true ) );
        assertThat( manager.getCacheConfiguration( nfc ).memory().evictionType(), equalTo( MEMORY ) );
        assertThat( manager.getCacheConfiguration( nfc ).expiration().wakeUpInterval(), equalTo( 900000L ) );

        assertThat( manager.getCacheConfiguration( local ).memory().size(), equalTo( 20971520L ) );

        assertThat( manager.getCacheConfiguration( folo_in_progress ).memory().size(), equalTo( 1000000L ) );
        assertThat( manager.getCacheConfiguration( folo_in_progress ).memory().evictionType(), equalTo( COUNT ) );
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
