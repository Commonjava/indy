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
import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.commonjava.indy.model.core.StoreType.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class RepositoryPathMaskExtTest
        extends AbstractContentManagementTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer();

    /**
     * Extensive tests. Both regex patterns and "directory prefix" patterns need to be tested, involving all three store types:
     * RemoteRepository (direct access)
     * HostedRepository (direct access)
     * Group (where one or more members has path masks)
     *  Case where two members contain the same path, but first one's path mask doesn't match
     *   - Should return content from the second member
     */
    @Test
    public void run()
            throws Exception
    {
        final String content1 = "{\"content1\": \"This is a test: " + System.nanoTime() + "\"}";
        final String content2 = "{\"content2\": \"This is a test: " + System.nanoTime() + "\"}";

        final String path_1 = "org/foo/foo-project/1.0/pom.xml";
        final String path_2 = "org/bar/bar-project/1.0/pom.xml";

        final String remote1 = "remote1";
        final String hosted1 = "hosted1";

        server.expect( server.formatUrl( remote1, path_1 ), 200, new ByteArrayInputStream( content1.getBytes() ) );

        RemoteRepository remoteRepo1 = new RemoteRepository( remote1, server.formatUrl( remote1 ) );
        Set<String> pathMaskPatterns = new HashSet<>();
        pathMaskPatterns.add("r|org/bar.*|"); // regex patterns
        remoteRepo1.setPathMaskPatterns(pathMaskPatterns);
        remoteRepo1 = client.stores().create( remoteRepo1, "adding remote 1", RemoteRepository.class );

        HostedRepository hostedRepo1 = new HostedRepository( hosted1 );
        pathMaskPatterns = new HashSet<>();
        pathMaskPatterns.add("org/foo");
        pathMaskPatterns.add("r|org/bar.*|");
        hostedRepo1.setPathMaskPatterns(pathMaskPatterns);
        hostedRepo1 = client.stores().create( hostedRepo1, "adding hosted 1", HostedRepository.class );
        client.content().store( hosted, hosted1, path_1, new ByteArrayInputStream( content2.getBytes() ) );
        client.content().store( hosted, hosted1, path_2, new ByteArrayInputStream( content2.getBytes() ) );

        Group g = new Group( "group1", remoteRepo1.getKey(), hostedRepo1.getKey() );
        g = client.stores().create( g, "adding group1", Group.class );
        System.out.printf( "\n\nGroup constituents are:\n  %s\n\n", StringUtils.join( g.getConstituents(), "\n  " ) );

        InputStream stream = null;
        String str = null;

        // Case 1. return content from the repo with valid mask

        // get stream for path-1 via group
        stream = client.content().get( group, g.getName(), path_1 );
        assertThat( stream, notNullValue() );

        // return content from the repo with mask
        str = IOUtils.toString( stream );
        stream.close();

        assertThat( str, equalTo( content2 ) );

        // Case 2. multiple repositories with same mask, use what can supply the real artifact

        // get stream for path-2 via group (success)
        stream = client.content().get( group, g.getName(), path_2 );
        assertThat( stream, notNullValue() );
        stream.close();

        // Case 3. test direct access hosted repo with regex mask

        // direct access for path-2 (success)
        stream = client.content().get( hosted, hostedRepo1.getName(), path_2 );
        assertThat( stream, notNullValue() );
        stream.close();

    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
