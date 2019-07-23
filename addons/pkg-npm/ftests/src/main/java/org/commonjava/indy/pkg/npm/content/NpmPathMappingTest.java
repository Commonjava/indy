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

import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * This case tests the npm path mapping solution for upstream and indy
 * when: <br />
 * <ul>
 *      <li>creates a remote repo, content with path :/project same as the upstream</li>
 *      <li>creates a hosted/group repos, contents with path :/project</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>retrieves the contents in different types of repo, path mapping will work</li>
 * </ul>
 */

public class NpmPathMappingTest
        extends AbstractContentManagementTest
{
    private static final String REMOTE = "REMOTE";

    private static final String HOSTED = "HOSTED";

    private static final String GROUP = "GROUP";

    private static final String PATH = "jquery";

    private static final String MAPPING_PATH = "jquery/package.json";

    private static final String CONTENT_1 = "This is content #1.";

    @Test
    public void test()
            throws Exception
    {
        server.expect( server.formatUrl( REMOTE, PATH ), 200,
                       new ByteArrayInputStream( CONTENT_1.getBytes( "UTF-8" ) ) );

        final RemoteRepository remote = new RemoteRepository( NPM_PKG_KEY, REMOTE, server.formatUrl( REMOTE ) );
        client.stores().create( remote, "adding npm remote repo", RemoteRepository.class );

        final HostedRepository hosted = new HostedRepository( NPM_PKG_KEY, HOSTED );
        client.stores().create( hosted, "adding npm hosted repo", HostedRepository.class );
        client.content().store( hosted.getKey(), PATH, new ByteArrayInputStream( CONTENT_1.getBytes() ) );

        final Group group = new Group( NPM_PKG_KEY, GROUP, remote.getKey(), hosted.getKey() );
        client.stores().create( group, "adding npm group repo", Group.class );

        assertThat( client.content().exists( remote.getKey(), MAPPING_PATH ), equalTo( false ) );
        assertThat( client.content().exists( remote.getKey(), PATH ), equalTo( true ) );

        assertThat( client.content().exists( hosted.getKey(), MAPPING_PATH ), equalTo( true ) );
        assertThat( client.content().exists( hosted.getKey(), PATH ), equalTo( true ) );

        assertThat( client.content().exists( group.getKey(), MAPPING_PATH ), equalTo( true ) );
        assertThat( client.content().exists( group.getKey(), PATH ), equalTo( true ) );

    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
