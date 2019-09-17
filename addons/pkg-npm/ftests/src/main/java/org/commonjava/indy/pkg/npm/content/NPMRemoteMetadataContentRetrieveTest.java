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
package org.commonjava.indy.pkg.npm.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * This case tests if package.json metadata can be retrieved correctly in a remote repo
 * when: <br />
 * <ul>
 *      <li>creates a remote repo and expect metadata files in it</li>
 *      <li>retrieve the files using corresponding mapping path in the remote repo</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the files content can be retrieved correctly with no error, equal to the original value</li>
 * </ul>
 */
public class NPMRemoteMetadataContentRetrieveTest
                extends AbstractContentManagementTest
{
    @Test
    public void test() throws Exception
    {
        final String packageContent =
                        "{\"name\": \"jquery\",\n" + "\"description\": \"JavaScript library for DOM operations\",\n" + "\"license\": \"MIT\"}";
        final String versionContent = "{\"name\": \"jquery\",\n" + "\"url\": \"jquery.com\",\n" + "\"version\": \"1.1.0\"}";

        final String packagePath = "jquery";
        final String versionPath = "jquery/1.1.0";

        server.expect( server.formatUrl( STORE, packagePath ), 200, new ByteArrayInputStream( packageContent.getBytes() ) );
        server.expect( server.formatUrl( STORE, versionPath ), 200, new ByteArrayInputStream( versionContent.getBytes() ) );

        final RemoteRepository remoteRepository = new RemoteRepository( NPM_PKG_KEY, STORE, server.formatUrl( STORE ) );
        final StoreKey storeKey = remoteRepository.getKey();

        client.stores().create( remoteRepository, "adding npm remote repo", RemoteRepository.class );

        final InputStream packageStream = client.content().get( storeKey, packagePath );
        final InputStream versionStream = client.content().get( storeKey, versionPath );

        assertThat( packageStream, notNullValue() );
        assertThat( versionStream, notNullValue() );

        assertThat( IOUtils.toString( packageStream ), equalTo( packageContent ) );
        assertThat( IOUtils.toString( versionStream ), equalTo( versionContent ) );

        packageStream.close();
        versionStream.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
