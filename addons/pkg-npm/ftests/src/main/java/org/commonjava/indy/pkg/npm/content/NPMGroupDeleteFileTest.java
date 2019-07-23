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

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * This case tests if files can be deleted in a group repo
 * when: <br />
 * <ul>
 *      <li>creates two hosted repos and two remote repos</li>
 *      <li>creates one group A repo contains two remote members</li>
 *      <li>creates one group B repo contains two members with one remote and one hosted</li>
 *      <li>creates one group C repo contains two hosted members</li>
 *      <li>delete file with original path / real path in the three group repos</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the file with original path can not be deleted for the group A</li>
 *     <li>the file with real path can be deleted for the group B</li>
 *     <li>the file with real path can be deleted for the group C</li>
 * </ul>
 */
public class NPMGroupDeleteFileTest
                extends AbstractContentManagementTest
{

    private static final String REPO_X = "X";

    private static final String REPO_Y = "Y";

    private static final String REPO_Z = "Z";

    private static final String REPO_W = "W";

    private static final String GROUP_A = "A";

    private static final String GROUP_B = "B";

    private static final String GROUP_C = "B";

    private static final String PATH = "jquery";

    private static final String REAL_PATH = "jquery/package.json";

    private static final String CONTENT_1 = "This is content #1.";

    private static final String CONTENT_2 = "This is content #2. Some more content, here.";

    @Test
    public void test() throws Exception
    {
        server.expect( server.formatUrl( REPO_X, PATH ), 200,
                       new ByteArrayInputStream( CONTENT_1.getBytes( "UTF-8" ) ) );

        server.expect( server.formatUrl( REPO_Y, PATH ), 200,
                       new ByteArrayInputStream( CONTENT_2.getBytes( "UTF-8" ) ) );

        final RemoteRepository repoX = new RemoteRepository( NPM_PKG_KEY, REPO_X, server.formatUrl( REPO_X ) );
        client.stores().create( repoX, "adding npm remote repo", RemoteRepository.class );

        final RemoteRepository repoY = new RemoteRepository( NPM_PKG_KEY, REPO_Y, server.formatUrl( REPO_Y ) );
        client.stores().create( repoY, "adding npm remote repo", RemoteRepository.class );

        final HostedRepository repoZ = new HostedRepository( NPM_PKG_KEY, REPO_Z );
        client.stores().create( repoZ, "adding npm hosted repo", HostedRepository.class );
        client.content().store( repoZ.getKey(), PATH, new ByteArrayInputStream( CONTENT_1.getBytes() ) );

        final HostedRepository repoW = new HostedRepository( NPM_PKG_KEY, REPO_W );
        client.stores().create( repoW, "adding npm hosted repo", HostedRepository.class );
        client.content().store( repoW.getKey(), PATH, new ByteArrayInputStream( CONTENT_2.getBytes() ) );

        final Group groupA = new Group( NPM_PKG_KEY, GROUP_A, repoX.getKey(), repoY.getKey() );
        client.stores().create( groupA, "adding npm group A repo", Group.class );

        final Group groupB = new Group( NPM_PKG_KEY, GROUP_B, repoX.getKey(), repoZ.getKey() );
        client.stores().create( groupB, "adding npm group B repo", Group.class );

        final Group groupC = new Group( NPM_PKG_KEY, GROUP_C, repoZ.getKey(), repoW.getKey() );
        client.stores().create( groupC, "adding npm group C repo", Group.class );

        assertThat( client.content().exists( groupA.getKey(), REAL_PATH ), equalTo( false ) );
        assertThat( client.content().exists( groupA.getKey(), PATH ), equalTo( true ) );

        assertThat( client.content().exists( groupB.getKey(), REAL_PATH ), equalTo( true ) );
        assertThat( client.content().exists( groupC.getKey(), REAL_PATH ), equalTo( true ) );

        client.content().delete( groupA.getKey(), PATH );
        client.content().delete( groupB.getKey(), REAL_PATH );
        client.content().delete( groupC.getKey(), REAL_PATH );

        assertThat( client.content().exists( groupA.getKey(), PATH ), equalTo( true ) );

        // We disable cascade deletion in DefaultContentManager.delete() by default.
        // As of now, we just leave out these two lines. Rui
        //assertThat( client.content().exists( groupB.getKey(), REAL_PATH ), equalTo( false ) );
        //assertThat( client.content().exists( groupC.getKey(), REAL_PATH ), equalTo( false ) );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
