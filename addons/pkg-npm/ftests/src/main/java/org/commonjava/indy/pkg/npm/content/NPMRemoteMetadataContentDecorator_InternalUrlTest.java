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
import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * This case tests if package.json metadata can be retrieved and DECORATED.
 * when: <br />
 * <ul>
 *      <li>creates remote repo A and expect metadata file in it</li>
 *      <li>creates group G containing A</li>
 *      <li>retrieve the metadata file from the remote repo A and group G</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the metadata content can be retrieved with all tarball urls decorated to proper context indy urls</li>
 * </ul>
 */
public class NPMRemoteMetadataContentDecorator_InternalUrlTest
                extends AbstractContentManagementTest
{
    protected static final String GROUP = "G";

    private static final String SUBPATH = "subpath";

    @Test
    public void test() throws Exception
    {

        final String packagePath = "jquery";

        final String baseUrl = server.formatUrl( STORE, SUBPATH );

        final String tarballUrl = baseUrl + "/jquery/-/jquery-1.5.1.tgz";

        final String packageContent = IOUtils.toString(
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "subpath-package-1.5.1.json" ) )
                                             .replace( "@@REGISTRY@@", baseUrl );

        server.expect( server.formatUrl( STORE, SUBPATH, packagePath ), 200,
                       new ByteArrayInputStream( packageContent.getBytes() ) );

        final RemoteRepository remoteRepository = new RemoteRepository( NPM_PKG_KEY, STORE, baseUrl );
        client.stores().create( remoteRepository, "adding npm remote repo", RemoteRepository.class );

        final Group group = new Group( NPM_PKG_KEY, GROUP, remoteRepository.getKey() );
        client.stores().create( group, "adding group", Group.class );

        // retrieve from remote repo
        StoreKey storeKey = remoteRepository.getKey();
        InputStream stream = client.content().get( storeKey, packagePath );
        assertThat( stream, notNullValue() );

        String contextUrl =
                UrlUtils.buildUrl( fixture.getUrl(), "content", NPM_PKG_KEY, storeKey.getType().name(), STORE );
        String decoratedContent = packageContent.replaceAll( tarballUrl, contextUrl + "/jquery/-/jquery-1.5.1.tgz" );
        assertThat( IOUtils.toString( stream ), equalTo( decoratedContent ) );
        stream.close();

        // retrieve from group G
        StoreKey groupKey = group.getKey();
        stream = client.content().get( groupKey, packagePath );
        assertThat( stream, notNullValue() );
        contextUrl = UrlUtils.buildUrl( fixture.getUrl(), "content", NPM_PKG_KEY, groupKey.getType().name(), GROUP );
        String maskedUrl = contextUrl + "/jquery/-/jquery-1.5.1.tgz";
        // group metadata is not a simple copy of the remote repo so we only check if the decorated tarball url exists
        assertThat( IOUtils.toString( stream ), containsString( maskedUrl ) );
        stream.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
