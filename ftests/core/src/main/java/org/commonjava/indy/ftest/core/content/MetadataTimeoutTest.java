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

import org.commonjava.indy.ftest.core.category.TimingDependent;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static java.lang.Thread.sleep;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Wait for the metadata.xml to expire. Then retrieve it and wait for it to expire, and check again.
 */
public class MetadataTimeoutTest
                extends AbstractMetadataTimeoutWorkingTest
{
    private final int METADATA_TIMEOUT_SECONDS = 1;

    private final int METADATA_TIMEOUT_WAITING_MILLISECONDS = 2000;

    @Test
    @Category( TimingDependent.class )
    public void timeout() throws Exception
    {
        // make the metadata.xml timeout
        sleep( METADATA_TIMEOUT_WAITING_MILLISECONDS );
        assertThat( "Metadata not removed when timeout", metadataFile.exists(), equalTo( false ) );

        // retrieve it again
        client.content().get( new StoreKey( MAVEN_PKG_KEY, remote, repoId ), metadataPath ).close();

        sleep( METADATA_TIMEOUT_WAITING_MILLISECONDS );
        assertThat( "(Second) Metadata not removed when timeout", metadataFile.exists(), equalTo( false ) );
    }

    @Override
    protected RemoteRepository createRemoteRepository()
    {
        RemoteRepository repository = new RemoteRepository( MAVEN_PKG_KEY, repoId, server.formatUrl( repoId ) );
        repository.setMetadataTimeoutSeconds( METADATA_TIMEOUT_SECONDS );
        return repository;
    }
}
