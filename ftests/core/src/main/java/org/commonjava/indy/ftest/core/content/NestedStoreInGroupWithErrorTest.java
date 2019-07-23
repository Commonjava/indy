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
import org.apache.http.HttpStatus;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>Correct remote repo1 which can return content correctly</li>
 *     <li>Incorrect remote repo2 which will return 401 error</li>
 *     <li>Group which contains these repos which the repo2 is the first member and repo1 is the last</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Client requests the content through group</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>The content can be fetched correctly from the group as repo1 has the content, even the repo2 returns 401 error</li>
 * </ul>
 */
public class NestedStoreInGroupWithErrorTest
        extends AbstractContentManagementTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    public void run()
            throws Exception
    {
        final String path = "org/foo/bar/pom.xml";

        final String repo1 = "repo1";
        final String content = "This is a test: " + System.nanoTime();
        final InputStream stream = new ByteArrayInputStream( content.getBytes() );
        server.expect( server.formatUrl( repo1, path ), 200, stream );
        RemoteRepository remote1 = new RemoteRepository( repo1, server.formatUrl( repo1 ) );
        remote1 = client.stores().create( remote1, "adding remote2", RemoteRepository.class );

        final String repo2 = "repo2";
        server.registerException( server.formatUrl( repo2, path ), "upstream error", HttpStatus.SC_UNAUTHORIZED );
        RemoteRepository remote2 = new RemoteRepository( repo2, server.formatUrl( repo2 ) );
        remote2.setMetadata( Location.CONNECTION_TIMEOUT_SECONDS, Integer.toString( 1 ) );
        remote2 = client.stores().create( remote2, "adding remote2", RemoteRepository.class );

        final String gName = "test";
        Group tgroup = new Group( gName, Arrays.asList( remote2.getKey(), remote1.getKey() ) );
        client.stores().create( tgroup, "new group with remote2", Group.class );
        try (InputStream is = client.content().get( group, gName, path ))
        {
            assertThat( IOUtils.toString( is ), equalTo( content ) );
        }
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
