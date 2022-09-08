/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class StoreSnapshotMetadataTest
                extends AbstractContentManagementTest
{
    final String store = "local-deployments";

    @Test
    public void storeFileAndVerifyExistence() throws Exception
    {
        final String changelog = "Create local-deployments";

        StoreKey storeKey = new StoreKey( MAVEN_PKG_KEY, hosted, store );

        final HostedRepository hosted = new HostedRepository( MAVEN_PKG_KEY, store );
        hosted.setAllowSnapshots( true );
        hosted.setAllowReleases( false );

        this.client.stores().create( hosted, changelog, HostedRepository.class );

        final InputStream stream = new ByteArrayInputStream( ( "This is a test: " + System.nanoTime() ).getBytes() );

        final String path = "org/commonjava/util/path-mapped-storage/0.1-SNAPSHOT/maven-metadata.xml";

        assertThat( client.content().exists( storeKey, path ), equalTo( false ) );

        client.content().store( storeKey, path, stream );

        assertThat( client.content().exists( storeKey, path ), equalTo( true ) );
    }

    protected boolean createStandardTestStructures()
    {
        return false;
    }

}
