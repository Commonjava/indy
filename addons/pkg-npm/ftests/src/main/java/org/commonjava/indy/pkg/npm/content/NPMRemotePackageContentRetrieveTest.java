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
import java.util.Random;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * This case tests if package.tgz can be retrieved correctly in a remote repo
 * when: <br />
 * <ul>
 *      <li>creates a remote repo and expect tgz file in it</li>
 *      <li>retrieve the file using corresponding mapping path in the remote repo</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the tgz content can be retrieved correctly with no error, equal to the original value</li>
 * </ul>
 */
public class NPMRemotePackageContentRetrieveTest
                extends AbstractContentManagementTest
{
    @Test
    public void test() throws Exception
    {
        byte[] tgz = new byte[32];
        new Random().nextBytes( tgz );

        final String tarballPath = "jquery/-/jquery-1.1.0.tgz";

        server.expect( server.formatUrl( STORE, tarballPath ), 200, new ByteArrayInputStream( tgz ) );

        final RemoteRepository remoteRepository = new RemoteRepository( NPM_PKG_KEY, STORE, server.formatUrl( STORE ) );
        final StoreKey storeKey = remoteRepository.getKey();

        client.stores().create( remoteRepository, "adding npm remote repo", RemoteRepository.class );

        final InputStream tarballStream = client.content().get( storeKey, tarballPath );

        assertThat( tarballStream, notNullValue() );
        assertThat( IOUtils.toByteArray( tarballStream ), equalTo( tgz ) );

        tarballStream.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
