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

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.helper.PathInfo;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class RemoteRepoHeadExistenceCheckTest
        extends AbstractContentManagementTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Ignore
    @Test
    public void run()
        throws Exception
    {
        final InputStream stream = new ByteArrayInputStream( ( "{\"content\": \"This is a test: " + System.nanoTime() + "\"}" ).getBytes() );
        final String path = "org/foo/foo-project/1/foo-1.txt";
        final String nonExistPath = "org/foo/foo-project/1/foo-2.txt";

        server.expect( server.formatUrl( STORE, path ), 200, stream );
        server.expect( server.formatUrl( STORE, nonExistPath ), 404, "not exist" );

        client.stores()
                .create( new RemoteRepository( STORE, server.formatUrl( STORE ) ), "adding test proxy",
                        RemoteRepository.class );

        final PathInfo result = client.content()
                .getInfo( remote, STORE, path );

        try
        {
            boolean exists = client.content().exists( remote, STORE, path );
            assertTrue(exists);
            File f = new File(new File(fixture.getBootOptions().getHomeDir()), "var/lib/indy/storage/remote-test/" + path);
            assertTrue(!f.exists());
            //TODO: this breaks because for whatever reason the head can not get content-length and we have to fall over to get().
            //Refer to NCL-2729 and we will have a better fix in future

            exists = client.content().exists( remote, STORE, nonExistPath );
            assertFalse(exists);
        }
        catch ( final IndyClientException e )
        {
            fail("IndyClientException: " + e);
        }

    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
