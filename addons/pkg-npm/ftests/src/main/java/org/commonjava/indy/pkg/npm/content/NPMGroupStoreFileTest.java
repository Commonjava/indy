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

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.util.ApplicationStatus;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * This case tests if files can be stored in a group repo
 * when: <br />
 * <ul>
 *      <li>creates two remote repos and one hosted repo</li>
 *      <li>creates one group A repo contains two remote members</li>
 *      <li>creates one group B repo contains two members with one remote and one hosted</li>
 *      <li>stores file in the two group repos</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the file can not be stored with 400 error for the group A</li>
 *     <li>the file can be stored with no error for the group B</li>
 * </ul>
 */
public class NPMGroupStoreFileTest
                extends AbstractContentManagementTest
{

    private static final String REPO_X = "X";

    private static final String REPO_Y = "Y";

    private static final String REPO_Z = "Z";

    private static final String GROUP_A = "A";

    private static final String GROUP_B = "B";

    private static final String PATH = "jquery";

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
        client.content().store( repoZ.getKey(), PATH, new ByteArrayInputStream( CONTENT_2.getBytes() ) );

        final Group groupA = new Group( NPM_PKG_KEY, GROUP_A, repoX.getKey(), repoY.getKey() );
        client.stores().create( groupA, "adding npm group A repo", Group.class );

        final Group groupB = new Group( NPM_PKG_KEY, GROUP_B, repoX.getKey(), repoZ.getKey() );
        client.stores().create( groupB, "adding npm group B repo", Group.class );

        final String update = "This is a test: " + System.nanoTime();

        try
        {
            client.content().store( groupA.getKey(), PATH, new ByteArrayInputStream( update.getBytes() ) );
        }
        catch ( IndyClientException e )
        {
            assertThat( e.getStatusCode(), equalTo( ApplicationStatus.BAD_REQUEST.code() ) );
        }

        client.content().store( groupB.getKey(), PATH, new ByteArrayInputStream( update.getBytes() ) );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
