/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.content.browse.client.IndyContentBrowseClientModule;
import org.commonjava.indy.content.browse.model.ContentBrowseResult;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verifies that the available packages are listed via browse api.
 * <br/>
 * Given:
 * <ul>
 *     <li>Content is stored in a NPM hosted repository</li>
 * </ul>
 * <br/>
 * When:
 * <ul>
 *     <li>A directory on the stored content's path is requested via GET.
 *        <br/>
 *        <ul>
 *          <li>directly from the hosted repository or from the scoped level</li>
 *          <li>without a trailing '/' or a '/index.html' in the requested path</li>
 *          <li>without using Accept: application/json</li>
 *        </ul>
 *      </li>
 * </ul>
 * <br/>
 * Then:
 * <ul>
 *     <li>The directory listing should be rendered to HTML</li>
 * </ul>
 */
public class StoreNPMThenListingDirTest
        extends AbstractContentManagementTest
{

    @Test
    public void run()
        throws Exception
    {

        final String changelog = "Create test structures";

        final HostedRepository hostedRepository =
                this.client.stores()
                        .create( new HostedRepository(NPMPackageTypeDescriptor.NPM_PKG_KEY, STORE ), changelog, HostedRepository.class );

        final String content = "This is a test: " + System.nanoTime();
        final InputStream stream = new ByteArrayInputStream( content.getBytes() );

        final String dirPath = "/@babel/opossum";
        final String path = dirPath + "/package.json";
        final String tarPath = dirPath + "/-/opossum-5.0.0.tgz";

        final StoreKey testKey = hostedRepository.getKey();

        assertThat( client.content().exists( testKey, tarPath ), equalTo( false ) );

        client.content().store( testKey, tarPath, stream );

        assertThat( client.content().exists( testKey, tarPath ), equalTo( true ) );

        final InputStream stream2 = new ByteArrayInputStream( content.getBytes() );
        assertThat( client.content().exists( testKey, path ), equalTo( false ) );

        client.content().store( testKey, path, stream2 );

        assertThat( client.content().exists( testKey, path ), equalTo( true ) );

        try(InputStream jsonIn = client.content().get( testKey, "@babel" ))
        {
            assertThat( jsonIn, notNullValue() );
            String json = IOUtils.toString( jsonIn );
            assertThat( json.startsWith( "{" ), equalTo( true ) );
            assertThat( json.contains( "/@babel/opossum/" ), equalTo( true ) );
        }

        try(InputStream jsonIn = client.content().get( testKey, "/" ))
        {
            assertThat( jsonIn, notNullValue() );
            String json = IOUtils.toString( jsonIn );
            assertThat( json.startsWith( "{" ), equalTo( true ) );
            assertThat( json.contains( "/@babel/" ), equalTo( true ) );
        }

        IndyContentBrowseClientModule browseClientModule = client.module( IndyContentBrowseClientModule.class );

        ContentBrowseResult browseResult = browseClientModule.getContentList(testKey, "/@babel/opossum");
        String browseResultInJson = new IndyObjectMapper(false).writeValueAsString(browseResult);

        assertThat( browseResultInJson.contains("/@babel/opossum/-/"), equalTo(true) );
        assertThat( "no metadata result", browseResult, notNullValue() );

        try(InputStream jsonIn = client.content().get( testKey, "/@babel/opossum" ))
        {
            assertThat( jsonIn, notNullValue() );
            String json = IOUtils.toString( jsonIn );
            assertEquals(content, json);
        }

        assertThat( client.content()
                          .exists( testKey, path ), equalTo( true ) );

    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.singletonList( new IndyContentBrowseClientModule() );
    }
}
