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
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test metadata timeout working when cache time interval is shorter than metadata timeout interval.
 * In this case normal content should be removed firstly before the metadata content.
 */
public class CacheFirstTimeoutWorkingTest
        extends AbstractMetadataTimeoutWorkingTest
{
    private static final int CACHE_TIMEOUT_SECONDS = 1;

    private static final int CACHE_TIMEOUT_WAITING_MILLISECONDS = 2000;

    private static final int METADATA_TIMEOUT_SECONDS = 3;

    private static final int METADATA_TIMEOUT_ADDITIONAL_WAITING_MILLISECONDS = 2000;

    @Test
    @Category( TimingDependent.class )
    public void timeout()
            throws Exception
    {
        logger.debug( "Starting sleep at: {}", new Date() );

        // make sure the non-metadata content times out
        Thread.sleep( CACHE_TIMEOUT_WAITING_MILLISECONDS );
        logger.debug( "Verifying content timeouts at: {} (timeout: {}s)", new Date(), CACHE_TIMEOUT_SECONDS );

        assertThat( "artifact should be removed when cache timeout", pomFile.exists(), equalTo( false ) );
        assertThat( "metadata should not be removed when cache timeout", metadataFile.exists(), equalTo( true ) );
        assertThat( "archetype should not be removed when cache timeout", archetypeFile.exists(),
                    equalTo( true ) );

        // make sure the metadata content times out
        Thread.sleep( METADATA_TIMEOUT_ADDITIONAL_WAITING_MILLISECONDS );
        logger.debug( "Verifying metadata timeouts at: {} (timeout: {}s)", new Date(), METADATA_TIMEOUT_SECONDS );

        assertThat( "metadata should be removed when metadata timeout", metadataFile.exists(), equalTo( false ) );
        assertThat( "archetype should be removed when metadata timeout", archetypeFile.exists(), equalTo( false ) );
    }

    @Override
    protected RemoteRepository createRemoteRepository()
    {
        final RemoteRepository repository = new RemoteRepository( repoId, server.formatUrl( repoId ) );
        repository.setCacheTimeoutSeconds( CACHE_TIMEOUT_SECONDS );
        repository.setMetadataTimeoutSeconds( METADATA_TIMEOUT_SECONDS );
        return repository;
    }
}
