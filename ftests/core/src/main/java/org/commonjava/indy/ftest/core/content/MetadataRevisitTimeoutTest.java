/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This case tests if remote maven metadata will timeout correctly based on the first time retrieval
 * Given: <br />
 * <ul>
 *      <li>remote repo A contains maven-metadata.xml</li>
 *      <li>metadata timeout is scheduled</li>
 * </ul>
 * When: <br />
 * <ul>
 *      <li>metadata retrieved via remote repo A by client</li>
 *      <li>wait for a period which does not pass the timeout window</li>
 *      <li>retrieved the package.json again</li>
 *      <li>wait for another period which passed the first accessed timeout window, but not the second</li>
 * </ul>
 * Then: <br />
 * <ul>
 *     <li>metadta expires with metadata timeout based on the first retrieval but not the second</li>
 * </ul>
 */
public class MetadataRevisitTimeoutTest
        extends AbstractMetadataTimeoutWorkingTest
{
    private final int METADATA_TIMEOUT_SECONDS = 3;

    private final int METADATA_TIMEOUT_WAITING_MILLISECONDS = 1000;

    @Test
    @Category( TimingDependent.class )
    public void timeout()
            throws Exception
    {
        // make the metadata.xml timeout
        sleepAndRunFileGC( METADATA_TIMEOUT_WAITING_MILLISECONDS * 4 );
        assertThat( "Metadata not removed when timeout", metadataFile.exists(), equalTo( false ) );

        // retrieve it again
        client.content().get( new StoreKey( MAVEN_PKG_KEY, remote, repoId ), metadataPath ).close();

        sleepAndRunFileGC( METADATA_TIMEOUT_WAITING_MILLISECONDS * 2 );
        //retrieve again
        client.content().get( new StoreKey( MAVEN_PKG_KEY, remote, repoId ), metadataPath ).close();
        sleepAndRunFileGC( METADATA_TIMEOUT_WAITING_MILLISECONDS * 2 );
        assertThat( "(Second) Metadata not removed when timeout", metadataFile.exists(), equalTo( false ) );
    }

    @Override
    protected RemoteRepository createRemoteRepository()
    {
        RemoteRepository repository = new RemoteRepository( MAVEN_PKG_KEY, repoId, server.formatUrl( repoId ) );
        repository.setMetadataTimeoutSeconds( METADATA_TIMEOUT_SECONDS );
        return repository;
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestConfig( fixture );
        writeConfigFile( "conf.d/trace.conf", "[honeycomb]\nenabled=false" );
    }
}
