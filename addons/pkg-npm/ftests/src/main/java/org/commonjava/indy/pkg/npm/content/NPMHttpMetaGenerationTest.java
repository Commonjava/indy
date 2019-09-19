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
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.maven.galley.transport.htcli.model.HttpExchangeMetadata;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * This case tests the http-meta json generation for npm remote and hosted repos, this meta is
 * necessary for npm header check when download
 * when: <br />
 * <ul>
 *      <li>creates a remote repo, with files in the remote repo</li>
 *      <li>creates a hosted repo, stores files in the hosted repo</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>*.http-metadata.json file will be generated correspondingly</li>
 * </ul>
 */

public class NPMHttpMetaGenerationTest
        extends AbstractContentManagementTest
{
    private static final String REMOTE = "REMOTE";

    private static final String HOSTED = "HOSTED";

    private static final String GROUP = "GROUP";

    private static final String PATH = "jquery";

    private static final String PACKAGE_HTTP_META_PATH = "jquery/package.json.http-metadata.json";

    private static final String TGZ_HTTP_META_PATH = "jquery/-/jquery-1.5.1.tgz.http-metadata.json";

    private static final String VERSION_HTTP_META_PATH = "jquery/1.5.1.http-metadata.json";

    private static final String CONTENT_1 = "This is content #1.";

    @Test
    public void test()
            throws Exception
    {
        final InputStream content =
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "package-1.5.1.json" );

        server.expect( server.formatUrl( REMOTE, PATH ), 200,
                       new ByteArrayInputStream( CONTENT_1.getBytes( "UTF-8" ) ) );

        final RemoteRepository remote = new RemoteRepository( NPM_PKG_KEY, REMOTE, server.formatUrl( REMOTE ) );
        client.stores().create( remote, "adding npm remote repo", RemoteRepository.class );

        final HostedRepository hosted = new HostedRepository( NPM_PKG_KEY, HOSTED );
        client.stores().create( hosted, "adding npm hosted repo", HostedRepository.class );
        client.content().store( hosted.getKey(), PATH, content );

        final Group group = new Group( NPM_PKG_KEY, GROUP, hosted.getKey() );
        client.stores().create( group, "adding npm group repo", Group.class );

        IndyObjectMapper mapper = new IndyObjectMapper( true );

        assertThat( client.content().exists( remote.getKey(), PATH ), equalTo( true ) );
        assertThat( client.content().exists( remote.getKey(), PACKAGE_HTTP_META_PATH ), equalTo( true ) );

        assertThat( client.content().exists( hosted.getKey(), PATH ), equalTo( true ) );
        assertThat( client.content().exists( hosted.getKey(), PACKAGE_HTTP_META_PATH ), equalTo( true ) );
        assertThat( client.content().exists( hosted.getKey(), TGZ_HTTP_META_PATH ), equalTo( true ) );
        assertThat( client.content().exists( hosted.getKey(), VERSION_HTTP_META_PATH ), equalTo( true ) );

        InputStream hostedStream = client.content().get( hosted.getKey(), PACKAGE_HTTP_META_PATH );
        HttpExchangeMetadata hostedMeta =
                mapper.readValue( IOUtils.toString( hostedStream ), HttpExchangeMetadata.class );
        assertThat( hostedMeta.getResponseHeaders().containsKey( "LAST-MODIFIED" ), equalTo( true ) );

        client.content().get( group.getKey(), PATH );
        assertThat( client.content().exists( group.getKey(), PACKAGE_HTTP_META_PATH ), equalTo( true ) );

        InputStream groupStream = client.content().get( group.getKey(), PACKAGE_HTTP_META_PATH );
        HttpExchangeMetadata groupMeta =
                mapper.readValue( IOUtils.toString( groupStream ), HttpExchangeMetadata.class );
        assertThat( groupMeta.getResponseHeaders().containsKey( "LAST-MODIFIED" ), equalTo( true ) );

        content.close();
        hostedStream.close();
        groupStream.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
