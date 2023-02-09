/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.model.util.HttpUtils;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.indy.util.ApplicationContent;
import org.commonjava.indy.util.ApplicationHeader;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Check if the repo proxy addon can correctly handle the remote indy listing rewrite function for head request
 * <br/>
 * GIVEN:
 * <ul>
 *     <li>Repo-proxy add-on enabled</li>
 *     <li>"Remote indy listing content rewrite" configuration enabled</li>
 * </ul>
 * <br/>
 * WHEN:
 * <ul>
 *     <li>Request path of directory listing with head method</li>
 * </ul>
 * <br/>
 * THEN:
 * <ul>
 *     <li>Response is from the configured remote indy with same directory path</li>
 *     <li>Response head content-length is using replaced content length</li>
 * </ul>
 */
public class RepoProxyRemoteIndyListingRewriteHeadTest
        extends AbstractIndyFunctionalTest
{
    private static final String REPO_NAME = "test";

    private final HostedRepository hosted = new HostedRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, REPO_NAME );

    private static final String PATH = "/foo/bar/";

    private final IndyObjectMapper mapper = new IndyObjectMapper( true );

    @Rule
    public ExpectationServer server = new ExpectationServer( "" );

    @Before
    public void setupRepos()
            throws Exception
    {
        server.expect( HttpMethod.HEAD, server.formatUrl( "api/browse/maven/hosted", REPO_NAME, PATH ),
                       ( request, response ) -> {
                           response.addHeader( ApplicationHeader.content_type.key(),
                                               ApplicationContent.application_json );
                           response.addHeader( ApplicationHeader.content_length.key(), Long.toString( 1 ) );
                           response.addHeader( ApplicationHeader.last_modified.key(),
                                               HttpUtils.formatDateHeader( new Date() ) );
                           response.addHeader( ApplicationHeader.md5.key(), "ThisIsFakeMD5" );
                           response.addHeader( ApplicationHeader.sha1.key(), "ThisIsFakeSHA1" );
                           response.setStatus( HttpServletResponse.SC_OK );
                       } );
    }

    @Test
    public void runHead()
            throws Exception
    {
        Map<String, String> headers =
                client.module( IndyContentBrowseClientModule.class ).headForContentList( hosted.getKey(), PATH );
        assertNotNull( headers );
        assertFalse( headers.isEmpty() );
        for ( Map.Entry<String, String> entry : headers.entrySet() )
        {
            logger.debug( "{} : {}", entry.getKey(), entry.getValue() );
        }
        assertThat( headers.get( ApplicationHeader.content_type.key().toLowerCase() ),
                    equalTo( ApplicationContent.application_json ) );
        assertThat( headers.get( ApplicationHeader.content_length.key().toLowerCase() ), equalTo( "1" ) );
        assertThat( headers.get( ApplicationHeader.md5.key().toLowerCase() ), equalTo( "ThisIsFakeMD5" ) );
        assertThat( headers.get( ApplicationHeader.sha1.key().toLowerCase() ), equalTo( "ThisIsFakeSHA1" ) );

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
