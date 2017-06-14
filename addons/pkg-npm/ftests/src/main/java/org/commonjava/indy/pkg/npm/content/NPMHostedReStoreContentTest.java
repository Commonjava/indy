/**
 * Copyright (C) 2017 Red Hat, Inc. (yma@commonjava.org)
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
package org.commonjava.indy.pkg.npm.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * This case test if files can be stored in a readonly hosted repo
 * when: <br />
 * <ul>
 *      <li>creates a readonly hosted repo</li>
 *      <li>stores file in hosted repo once</li>
 *      <li>updates the hosted repo to non-readonly</li>
 *      <li>stores file again</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the file can not be stored with 405 error first time</li>
 *     <li>the file can be stored successfully with no error second time</li>
 * </ul>
 */
public class NPMHostedReStoreContentTest
                extends AbstractContentManagementTest
{

    @Test
    public void test() throws Exception
    {
        final String versionContent =
                        "{\"name\": \"jquery\",\n" + "\"url\": \"jquery.com\",\n" + "\"version\": \"2.1.0\"}";
        InputStream stream = new ByteArrayInputStream( versionContent.getBytes() );

        final String path = "jquery/2.1.0/package.json";
        final String mappingPath = "jquery/2.1.0";

        final String repoName = "test-hosted";
        HostedRepository repo = new HostedRepository( NPM_PKG_KEY, repoName );

        repo = client.stores().create( repo, "adding npm hosted repo", HostedRepository.class );

        StoreKey storeKey = repo.getKey();
        assertThat( client.content().exists( storeKey, mappingPath ), equalTo( false ) );

        client.content().store( storeKey, path, stream );
        assertThat( client.content().exists( storeKey, mappingPath ), equalTo( true ) );

        final String versionSnapshotContent =
                        "{\"name\": \"jquery\",\n" + "\"url\": \"jquery.com\",\n" + "\"version\": \"2.2.0-snapshot\"}";
        InputStream snapshotStream = new ByteArrayInputStream( versionSnapshotContent.getBytes() );

        client.content().store( storeKey, path, snapshotStream );
        assertThat( client.content().exists( storeKey, mappingPath ), equalTo( true ) );

        final InputStream is = client.content().get( storeKey, mappingPath );
        final String result = IOUtils.toString( is );

        assertThat( result, equalTo( versionSnapshotContent ) );

        is.close();
    }
}
