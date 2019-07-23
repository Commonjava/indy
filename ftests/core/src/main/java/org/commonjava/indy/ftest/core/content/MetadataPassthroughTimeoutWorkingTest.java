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
package org.commonjava.indy.ftest.core.content;

import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.ftest.core.category.TimingDependent;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
/**
 * Test metadata timeout working passthrough is set to true.
 * In this the passthrough timeout(default config timeout) will work, so all contents should be removed
 * only when passthrough timeout arrives, even the metadata timeout and normal timeout is set.
 * 
 */
public class MetadataPassthroughTimeoutWorkingTest
        extends AbstractMetadataTimeoutWorkingTest
{
    private final int CACHE_TIMEOUT_SECONDS = 1;

    private final int CACHE_TIMEOUT_WAITING_MILLISECONDS = getTestTimeoutMultiplier() * 1500;

    private final int METADATA_TIMEOUT_SECONDS = 4;

    private final int METADATA_TIMEOUT_WAITING_MILLISECONDS = getTestTimeoutMultiplier() * 4000;

    private final int PASSTHROUGH_TIMEOUT_SECONDS = 9;

    private final int PASSTHROUGH_TIMEOUT_WAITING_MILLISECONDS = getTestTimeoutMultiplier() * 5000;

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        writeConfigFile( "main.conf", "passthrough.timeout=" + PASSTHROUGH_TIMEOUT_SECONDS + "\n" + readTestResource(
                "default-test-main.conf" ) );
    }

    @Test
    @Category( TimingDependent.class )
    public void timeout()
            throws Exception
    {
        Thread.sleep( CACHE_TIMEOUT_WAITING_MILLISECONDS );
        logger.debug( "Verifying content timeouts at: {} (timeout: {}s)", new Date(), CACHE_TIMEOUT_SECONDS );
        assertThat( "artifact should not be removed because of passthrough", pomFile.exists(), equalTo( true ) );
        assertThat( "metadata should not be removed because of passthrough", metadataFile.exists(), equalTo( true ) );
        assertThat( "archetype should not be removed because of passthrough", archetypeFile.exists(), equalTo( true ) );

        Thread.sleep( METADATA_TIMEOUT_WAITING_MILLISECONDS );
        logger.debug( "Verifying metadata timeouts at: {} (timeout: {}s)", new Date(), METADATA_TIMEOUT_SECONDS );
        assertThat( "artifact should not be removed because of passthrough", pomFile.exists(), equalTo( true ) );
        assertThat( "metadata should not be removed because of passthrough", metadataFile.exists(), equalTo( true ) );
        assertThat( "archetype should not be removed because of passthrough", archetypeFile.exists(), equalTo( true ) );

        Thread.sleep( PASSTHROUGH_TIMEOUT_WAITING_MILLISECONDS );
        logger.debug( "Verifying passthrough timeouts at: {} (timeout: {}s)", new Date(), PASSTHROUGH_TIMEOUT_SECONDS );
        assertThat( "artifact should be removed because of passthrough timeout", pomFile.exists(), equalTo( false ) );
        assertThat( "metadata should be removed because of passthrough timeout", metadataFile.exists(),
                    equalTo( false ) );
        assertThat( "archetype should be removed because of passthrough timeout", archetypeFile.exists(),
                    equalTo( false ) );
    }

    @Override
    protected RemoteRepository createRemoteRepository()
    {
        final RemoteRepository repository = new RemoteRepository( repoId, server.formatUrl( repoId ) );
        repository.setPassthrough( true );
        repository.setCacheTimeoutSeconds( CACHE_TIMEOUT_SECONDS );
        repository.setMetadataTimeoutSeconds( METADATA_TIMEOUT_SECONDS );
        return repository;
    }
}
