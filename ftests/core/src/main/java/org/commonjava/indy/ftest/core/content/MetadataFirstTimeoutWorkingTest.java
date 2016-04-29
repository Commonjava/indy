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
package org.commonjava.indy.ftest.core.content;

import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class MetadataFirstTimeoutWorkingTest
        extends AbstractMetadataTimeoutWorkingTest
{
    private static final int METADATA_TIMEOUT_SECONDS = 2;

    private static final int METADATA_TIMEOUT_WAITING_MILLISECONDS = 3000;

    private static final int CACHE_TIMEOUT_SECONDS = 7;

    private static final int CACHE_TIMEOUT_WAITING_MILLISECONDS = 5000;

    @Test
    public void timeout()
            throws Exception
    {
        // make sure the metadata timout
        Thread.sleep( METADATA_TIMEOUT_WAITING_MILLISECONDS );
        logger.debug( "Timeout time {}s passed!", METADATA_TIMEOUT_SECONDS );
        final File metadataFileAgain = new File( metadataFilePath );
        assertThat( "metadata should be removed when metadata timeout", metadataFileAgain.exists(), equalTo( false ) );
        final File archetypeFileAgain = new File( archetypeFilePath );
        assertThat( "archetype should be removed when metadata timeout", archetypeFileAgain.exists(),
                    equalTo( false ) );
        File pomFileAgain = new File( pomFilePath );
        assertThat( "artifact should not be removed when metadata timeout", pomFileAgain.exists(), equalTo( true ) );

        // make sure the repo timout
        Thread.sleep( CACHE_TIMEOUT_WAITING_MILLISECONDS );
        logger.debug( "Timeout time {}s passed!", CACHE_TIMEOUT_SECONDS );
        pomFileAgain = new File( pomFilePath );
        assertThat( "artifact should be removed when cache timeout", pomFileAgain.exists(), equalTo( false ) );
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
