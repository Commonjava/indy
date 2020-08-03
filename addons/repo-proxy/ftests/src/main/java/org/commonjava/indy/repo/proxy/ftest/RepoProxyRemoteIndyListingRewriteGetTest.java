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
package org.commonjava.indy.repo.proxy.ftest;

import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.content.browse.client.IndyContentBrowseClientModule;
import org.commonjava.indy.content.browse.model.ContentBrowseResult;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Check if the repo proxy addon can correctly handle the remote indy listing rewrite function for get request
 * <br/>
 * GIVEN:
 * <ul>
 *     <li>Repo-proxy add-on enabled</li>
 *     <li>"Remote indy listing content rewrite" configuration enabled</li>
 * </ul>
 * <br/>
 * WHEN:
 * <ul>
 *     <li>Request path of directory listing</li>
 * </ul>
 * <br/>
 * THEN:
 * <ul>
 *     <li>Response is from the configured remote indy with same directory path</li>
 *     <li>Response content is using replaced host info for url related info</li>
 * </ul>
 */
public class RepoProxyRemoteIndyListingRewriteGetTest
        extends AbstractIndyFunctionalTest
{
    private static final String REPO_NAME = "test";

    private final HostedRepository hosted = new HostedRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, REPO_NAME );

    private static final String PATH = "/foo/bar/";

    private final IndyObjectMapper mapper = new IndyObjectMapper( true );

    @Rule
    public ExpectationServer server = new ExpectationServer( "" );

    private String expectedRemoteContent;

    @Before
    public void setupRepos()
            throws Exception
    {
        expectedRemoteContent = TestUtils.getExpectedRemoteContent( server, hosted, PATH, mapper );
        server.expect( server.formatUrl( "api/browse/maven/hosted", REPO_NAME, PATH ), 200,
                       new ByteArrayInputStream( expectedRemoteContent.getBytes() ) );
    }

    @Test
    public void runGet()
            throws Exception
    {
        ContentBrowseResult rewrittenResult =
                client.module( IndyContentBrowseClientModule.class ).getContentList( hosted.getKey(), PATH );
        assertNotNull( rewrittenResult );
        ContentBrowseResult originalResult = mapper.readValue( expectedRemoteContent, ContentBrowseResult.class );
        assertThat( rewrittenResult.getStoreKey(), equalTo( originalResult.getStoreKey() ) );
        assertThat( rewrittenResult.getPath(), equalTo( originalResult.getPath() ) );
        assertThat( rewrittenResult.getParentPath(), equalTo( originalResult.getParentPath() ) );

        assertThat( rewrittenResult.getStoreBrowseUrl(), not( originalResult.getStoreBrowseUrl() ) );
        assertThat( rewrittenResult.getStoreContentUrl(), not( originalResult.getStoreContentUrl() ) );
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        final String url = server.formatUrl( "" );
        logger.debug( "Remote url: {}", url );
        writeConfigFile( "conf.d/repo-proxy.conf", "[repo-proxy]\nenabled=true\napi.methods=GET,HEAD\n"
                + "remote.indy.listing.rewrite.enabled=true\nremote.indy.url=" + url );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.singletonList( new IndyContentBrowseClientModule() );
    }

}
