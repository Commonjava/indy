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
package org.commonjava.indy.content.browse.ftest;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Verifies that nothing happens to stored content when an implied directory listing is requested.
 * <br/>
 * Given:
 * <ul>
 *     <li>Content is stored in a hosted repository</li>
 *     <li>Hosted repository is a member of a group</li>
 * </ul>
 * <br/>
 * When:
 * <ul>
 *     <li>A directory on the stored content's path is requested via GET.
 *        <br/>
 *        <ul>
 *          <li>from the group</li>
 *          <li>without a trailing '/' or a '/index.html' in the requested path</li>
 *          <li>without using Accept: application/json</li>
 *        </ul>
 *      </li>
 * </ul>
 * <br/>
 * Then:
 * <ul>
 *     <li>The directory listing should be rendered to HTML</li>
 *     <li>The stored content should be unaffected</li>
 * </ul>
 */
public class StoreThenGetUnSuffixedDirAndDownloadViaGroupTest
        extends AbstractContentManagementTest
{

    @Test
    public void run()
        throws Exception
    {
        final String content = "This is a test: " + System.nanoTime();
        final InputStream stream = new ByteArrayInputStream( content.getBytes() );

        final String dirPath = "/org/foo/bar/1";
        final String path = dirPath + "/bar-1.pom";

        assertThat( client.content()
                          .exists( hosted, STORE, path ), equalTo( false ) );

        client.content()
              .store( hosted, STORE, path, stream );

        assertThat( client.content()
                          .exists( group, PUBLIC, path ), equalTo( true ) );

        try(InputStream jsonIn = client.content().get( new StoreKey( group, PUBLIC ), dirPath ))
        {
            assertThat( jsonIn, notNullValue() );
            String json = IOUtils.toString( jsonIn );
            assertThat( json.startsWith( "{" ), equalTo( true ) );
            assertThat( json.contains( "bar-1.pom" ), equalTo( true ) );
        }

        assertThat( client.content()
                          .exists( group, PUBLIC, path ), equalTo( true ) );

        final URL url = new URL( client.content()
                                       .contentUrl( hosted, STORE, path ) );

        final InputStream is = url.openStream();

        final String result = IOUtils.toString( is );
        is.close();

        assertThat( result, equalTo( content ) );

    }
}
