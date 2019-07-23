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
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
/**
 * Test metadata timeout working when cache time interval is longer than metadata timeout interval.
 * In this case normal content should be removed after the metadata content.
 */
public class MetadataFirstTimeoutWorkingTest
        extends AbstractMetadataTimeoutWorkingTest
{
    private final int METADATA_TIMEOUT_SECONDS = 2;

    private final int METADATA_TIMEOUT_WAITING_MILLISECONDS = getTestTimeoutMultiplier() * 3000;

    private final int CACHE_TIMEOUT_SECONDS = 7;

    private final int CACHE_TIMEOUT_WAITING_MILLISECONDS = getTestTimeoutMultiplier() * 5000;

    @Test
    @Category( TimingDependent.class )
    public void timeout()
            throws Exception
    {
        // make sure the metadata timout
        Thread.sleep( METADATA_TIMEOUT_WAITING_MILLISECONDS );
        logger.debug( "Timeout time {}s passed!", METADATA_TIMEOUT_SECONDS );
        assertThat( "metadata should be removed when metadata timeout", metadataFile.exists(), equalTo( false ) );
        assertThat( "archetype should be removed when metadata timeout", archetypeFile.exists(),
                    equalTo( false ) );
        assertThat( "artifact should not be removed when metadata timeout", pomFile.exists(), equalTo( true ) );

        // make sure the repo timout
        Thread.sleep( CACHE_TIMEOUT_WAITING_MILLISECONDS );
        logger.debug( "Timeout time {}s passed!", CACHE_TIMEOUT_SECONDS );
        assertThat( "artifact should be removed when cache timeout", pomFile.exists(), equalTo( false ) );
    }

    @Override
    protected RemoteRepository createRemoteRepository()
    {
        final RemoteRepository repository = new RemoteRepository( repoId, server.formatUrl( repoId ) );
        repository.setMetadataTimeoutSeconds( METADATA_TIMEOUT_SECONDS );
        repository.setCacheTimeoutSeconds( CACHE_TIMEOUT_SECONDS );
        return repository;
    }
}
