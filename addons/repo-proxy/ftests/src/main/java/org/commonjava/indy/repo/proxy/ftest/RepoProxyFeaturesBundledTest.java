/**
 * Copyright (C) 2020 Red Hat, Inc.
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
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.content.browse.client.IndyContentBrowseClientModule;
import org.commonjava.indy.content.browse.model.ContentBrowseResult;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_NPM;
import static org.commonjava.maven.galley.util.PathUtils.normalize;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Check if the repo proxy addon can work correctly for several supported features enabled together, including:
 * <ol>
 *     <li>Artifact download for hosted repo proxy</li>
 *     <li>Directory listing response rewrite to remote indy</li>
 *     <li>Npm metadata rewrite</li>
 *     <li>Block list</li>
 * </ol>
 */
public class RepoProxyFeaturesBundledTest
        extends AbstractIndyFunctionalTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer( "" );

    private final IndyObjectMapper mapper = new IndyObjectMapper( true );

    private static final String REPO_NAME_MVN = "test";

    private static final String REPO_NAME_NPM = "npmjs";

    private final HostedRepository mvnHosted =
            new HostedRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, REPO_NAME_MVN );

    private final HostedRepository npmHosted = new HostedRepository( PKG_TYPE_NPM, REPO_NAME_NPM );

    private static final String PATH_ARTIFACT = "foo/bar/1.0/foo-bar-1.0.txt";

    private static final String ARTIFACT_CONTENT = "This is foo bar 1.0 content";

    private static final String PATH_DIR = "/foo/bar/";

    private static final String PATH_NPM_ROOT = "jquery";

    private static final String PATH_BLOCKED = "/org/apache/plugins/maven-metadata.xml";

    private static final String PATH_BLOCKED_CONTENT = "This is content blocked";

    //@formatter:off
    private static final String CONTENT_NPM_TEMPLATE = "{"
            + "\"_id\": \"jquery\","
            + "\"versions\":{"
            + "\"1.5.1\": {"
            + "\"dist\": {"
            + "\"shasum\": \"2ae2d661e906c1a01e044a71bb5b2743942183e5\","
            + "\"tarball\": \"%s/jquery/-/jquery-1.5.1.tgz\""
            + "}"
            + "}"
            + "}"
            + "}";
    //@formatter:on

    private String REMOTE_NPM_REPO_PATH;

    private String HOSTED_NPM_REPO_PATH;

    @Before
    public void setupRepos()
            throws Exception
    {
        // Prepare mvn artifact remote content
        server.expect( server.formatUrl( REPO_NAME_MVN, PATH_ARTIFACT ), 200,
                       new ByteArrayInputStream( ARTIFACT_CONTENT.getBytes() ) );
        client.stores()
              .create( new RemoteRepository( PKG_TYPE_MAVEN, REPO_NAME_MVN, server.formatUrl( REPO_NAME_MVN ) ),
                       "remote pnc-builds", RemoteRepository.class );

        // Prepare mvn directory listing remote content
        server.expect( server.formatUrl( "api/browse/maven/hosted", REPO_NAME_MVN, PATH_DIR ), 200,
                       new ByteArrayInputStream(
                               TestUtils.getExpectedRemoteContent( server, mvnHosted, PATH_DIR, mapper ).getBytes() ) );

        // Prepare npm metadata content
        final String contentNPM = String.format( CONTENT_NPM_TEMPLATE, server.formatUrl( REPO_NAME_NPM ) );
        server.expect( server.formatUrl( REPO_NAME_NPM, PATH_NPM_ROOT ), 200,
                       new ByteArrayInputStream( contentNPM.getBytes() ) );
        final RemoteRepository npmRemote = client.stores()
              .create( new RemoteRepository( PKG_TYPE_NPM, REPO_NAME_NPM, server.formatUrl( REPO_NAME_NPM ) ),
                       "remote npmjs", RemoteRepository.class );
        REMOTE_NPM_REPO_PATH = normalize( fixture.getUrl(), "content/npm/remote", npmRemote.getName() );
        HOSTED_NPM_REPO_PATH = normalize( fixture.getUrl(), "content/npm/hosted", npmHosted.getName() );

        // Prepare for block path
        server.expect( server.formatUrl( REPO_NAME_MVN, PATH_BLOCKED ), 200, new ByteArrayInputStream( PATH_BLOCKED_CONTENT.getBytes() ) );

    }

    @Test
    public void run()
            throws Exception
    {
        // For maven artifact proxy
        try (InputStream result = client.content().get( mvnHosted.getKey(), PATH_ARTIFACT ))
        {
            assertThat( result, notNullValue() );
            final String content = IOUtils.toString( result );
            assertThat( content, equalTo( ARTIFACT_CONTENT ) );
        }

        // For content listing response rewrite
        ContentBrowseResult rewrittenResult =
                client.module( IndyContentBrowseClientModule.class ).getContentList( mvnHosted.getKey(), PATH_DIR );
        assertNotNull( rewrittenResult );
        final String originalContent = TestUtils.getExpectedRemoteContent( server, mvnHosted, PATH_DIR, mapper );
        ContentBrowseResult originalResult = mapper.readValue( originalContent, ContentBrowseResult.class );
        assertThat( rewrittenResult.getStoreKey(), equalTo( originalResult.getStoreKey() ) );
        assertThat( rewrittenResult.getPath(), equalTo( originalResult.getPath() ) );
        assertThat( rewrittenResult.getParentPath(), equalTo( originalResult.getParentPath() ) );
        assertThat( rewrittenResult.getStoreBrowseUrl(), not( originalResult.getStoreBrowseUrl() ) );
        assertThat( rewrittenResult.getStoreContentUrl(), not( originalResult.getStoreContentUrl() ) );

        // For npm metadata rewrite
        try (InputStream result = client.content().get( npmHosted.getKey(), PATH_NPM_ROOT ))
        {
            assertThat( result, notNullValue() );
            final String content = IOUtils.toString( result );
            logger.debug( "NPM Rewrite content: {}", content );
            assertThat( content, containsString( HOSTED_NPM_REPO_PATH ) );
            assertThat( content.contains( REMOTE_NPM_REPO_PATH ), equalTo( false ) );
        }

        // For block path
        try (InputStream result = client.content().get( mvnHosted.getKey(), PATH_BLOCKED ))
        {
            assertThat( result, nullValue() );
        }
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        final String url = server.formatUrl( "" );
        writeConfigFile( "conf.d/repo-proxy.conf", "[repo-proxy]\nenabled=true" + "\napi.methods=GET,HEAD"
                + "\nblock.path.patterns=/org/apache/plugins/maven-metadata.xml\n"
                + "remote.indy.listing.rewrite.enabled=true\nremote.indy.url=" + url );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.singletonList( new IndyContentBrowseClientModule() );
    }
}
