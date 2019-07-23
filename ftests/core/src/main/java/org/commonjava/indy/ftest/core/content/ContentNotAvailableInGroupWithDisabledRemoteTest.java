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
package org.commonjava.indy.ftest.core.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>A group contains a remote</li>
 *     <li>Content in the remote</li>
 *     <li>Content available through group via remote</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Remote set disabled</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>Content not available in group</li>
 * </ul>
 */
public class ContentNotAvailableInGroupWithDisabledRemoteTest
        extends AbstractIndyFunctionalTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    public void contentAccess()
            throws Exception
    {
        final String remoteName = "r";
        final String groupName = "g";
        final String path = "org/foo/bar/foo-bar-1.txt";
        final String content = "This is a test";

        server.expect( server.formatUrl( remoteName, path ), 200, content );

        RemoteRepository r = new RemoteRepository( remoteName, server.formatUrl( remoteName ) );
        r = client.stores().create( r, "adding remote", RemoteRepository.class );

        Group g = new Group( groupName, r.getKey() );
        g = client.stores().create( g, "adding group", Group.class );

        try (InputStream in = client.content().get( g.getKey(), path ))
        {
            assertThat( IOUtils.toString( in ), equalTo( content ) );
        }

        r.setDisabled( true );
        client.stores().update( r, "adding remote" );

        assertThat( client.content().exists( g.getKey(), path ), equalTo( false ) );
    }
}
