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

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.content.browse.model.ContentBrowseResult;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.repo.proxy.RepoProxyUtils;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.maven.galley.util.PathUtils;
import org.commonjava.test.http.expect.ExpectationServer;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Check if the repo proxy addon can correcly handle the remote indy listing rewrite function
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
 *     <li>Response is from the configured remote indy with same direcoty path</li>
 *     <li>Response content is using replaced host info for url related info</li>
 * </ul>
 */
public class RepoProxyRemoteIndyListingRewriteTest
        extends AbstractIndyFunctionalTest
{
    private static final String REPO_NAME = "test";

    private final HostedRepository hosted = new HostedRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, REPO_NAME );

    private static final String PATH = "/foo/bar/";

    private static final String PATH_WITHOUT_LAST_SLASH = "/foo/bar";

    private final IndyObjectMapper mapper = new IndyObjectMapper( true );

    @Rule
    public ExpectationServer server = new ExpectationServer( "" );

    @Before
    public void setupRepos()
            throws Exception
    {
        server.expect( server.formatUrl( "api/browse/maven/hosted", REPO_NAME, PATH_WITHOUT_LAST_SLASH ), 200,
                       new ByteArrayInputStream( getExpectedRemoteContent().getBytes() ) );
    }

    @Test
    public void run()
            throws Exception
    {
        try (InputStream result = client.content().get( hosted.getKey(), PATH ))
        {
            assertThat( result, notNullValue() );
            final String content = IOUtils.toString( result );
            ContentBrowseResult rewrittenResult = mapper.readValue( content, ContentBrowseResult.class );
            final String originalContent = getExpectedRemoteContent();
            ContentBrowseResult originalResult = mapper.readValue( originalContent, ContentBrowseResult.class );
            assertThat( rewrittenResult.getStoreKey(), equalTo( originalResult.getStoreKey() ) );
            assertThat( rewrittenResult.getPath(), equalTo( originalResult.getPath() ) );
            assertThat( rewrittenResult.getParentPath(), equalTo( originalResult.getParentPath() ) );

            assertThat( rewrittenResult.getStoreBrowseUrl(), not( originalResult.getStoreBrowseUrl() ) );
            assertThat( rewrittenResult.getStoreContentUrl(), not( originalResult.getStoreContentUrl() ) );
        }
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

    private String getExpectedRemoteContent()
            throws IOException
    {
        final String url = server.formatUrl( "" );
        ContentBrowseResult result = new ContentBrowseResult();
        result.setStoreKey( hosted.getKey() );
        final String rootStorePath =
                PathUtils.normalize( url, "api/browse", hosted.getKey().toString().replaceAll( ":", "/" ) );
        result.setParentUrl( PathUtils.normalize( rootStorePath, "foo", "/" ) );
        result.setParentPath( "foo/" );
        result.setPath( "foo/bar/" );
        result.setStoreBrowseUrl( rootStorePath );
        result.setStoreContentUrl(
                PathUtils.normalize( url, "api/content", hosted.getKey().toString().replaceAll( ":", "/" ) ) );
        result.setSources( Collections.singletonList( rootStorePath ) );
        ContentBrowseResult.ListingURLResult listResult = new ContentBrowseResult.ListingURLResult();
        final String path = PATH + "foo-bar.txt";
        listResult.setListingUrl( PathUtils.normalize( rootStorePath, path ) );
        listResult.setPath( path );
        Set<String> sources = new HashSet<>();
        sources.add( "indy:" + hosted.getKey().toString() + path );
        listResult.setSources( sources );
        result.setListingUrls( Collections.singletonList( listResult ) );

        return mapper.writeValueAsString( result );

    }
}
