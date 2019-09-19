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
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class RepositoryPathMaskTest
        extends AbstractContentManagementTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    public void run()
        throws Exception
    {
        final String content = "{\"content\": \"This is a test: " + System.nanoTime() + "\"}";
        final String path_1 = "org/foo/foo-project/1/a.out.txt";
        final String path_2 = "org/bar/bar-project/1/a.out.txt"; // not in masks

        final String repo1 = "repo1";

        server.expect( server.formatUrl( repo1, path_1 ), 200, new ByteArrayInputStream( content.getBytes() ) );
        server.expect( server.formatUrl( repo1, path_2 ), 200, new ByteArrayInputStream( content.getBytes() ) );

        RemoteRepository remote1 = new RemoteRepository( repo1, server.formatUrl( repo1 ) );

        Set<String> pathMaskPatterns = new HashSet<>();
        pathMaskPatterns.add("org/foo");
        remote1.setPathMaskPatterns(pathMaskPatterns);

        remote1 = client.stores()
                .create( remote1, "adding remote", RemoteRepository.class );

        Group g = new Group( "test", remote1.getKey() );
        g = client.stores()
                .create( g, "adding group", Group.class );

        System.out.printf( "\n\nGroup constituents are:\n  %s\n\n", StringUtils.join( g.getConstituents(), "\n  " ) );

        // get stream for path-1 via group (success)
        InputStream stream = client.content()
                .get( group, g.getName(), path_1 );

        assertThat( stream, notNullValue() );

        final String str = IOUtils.toString( stream );
        assertThat( str, equalTo( content ) );
        stream.close();

        // get stream for path_2 via group (null)
        stream = client.content()
                .get( group, g.getName(), path_2 );

        assertThat( stream, nullValue() );

        // get stream for path_1 via concrete store (success)
        stream = client.content()
                .get( remote, remote1.getName(), path_1 );

        assertThat( stream, notNullValue() );
        stream.close();

        // get stream for path_2 via concrete repo (null)
        stream = client.content()
                .get( remote, remote1.getName(), path_2 );

        assertThat( stream, nullValue() );

    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
