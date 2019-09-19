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

import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class GroupIgnoresDisabledRemoteRepositoryTest
        extends AbstractContentManagementTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    public void run()
        throws Exception
    {
        final String repo1 = "repo1";
        final String repo2 = "repo2";
        final String path = "path/to/test.txt";

        String disabledContent = "This is disabled.";
        String enabledContent = "This is enabled.";

        server.expect( server.formatUrl( repo1, path ), 200, disabledContent );
        server.expect( server.formatUrl( repo2, path ), 200, enabledContent );

        RemoteRepository remote1 = new RemoteRepository( repo1, server.formatUrl( repo1 ) );
        remote1.setDisabled( true );

        remote1 = client.stores()
                        .create( remote1, "adding remote", RemoteRepository.class );

        RemoteRepository remote2 = new RemoteRepository( repo2, server.formatUrl( repo2 ) );

        remote2 = client.stores()
                        .create( remote2, "adding remote", RemoteRepository.class );

        Group g = new Group( "test", remote1.getKey(), remote2.getKey() );
        g = client.stores()
                  .create( g, "adding group", Group.class );

        System.out.printf( "\n\nGroup constituents are:\n  %s\n\n", StringUtils.join( g.getConstituents(), "\n  " ) );

        final InputStream stream = client.content()
                                         .get( group, g.getName(), path );

        assertThat( stream, notNullValue() );

        final String content = IOUtils.toString( stream );
        assertThat( content, equalTo( enabledContent ) );
        stream.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
