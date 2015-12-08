/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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

import static org.commonjava.indy.model.core.StoreType.group;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.test.http.TestHttpServer;
import org.junit.Rule;
import org.junit.Test;

public class MetadataMergeRepoNoMetaListingTimeoutTest
    extends AbstractContentManagementTest
{

    public class DelayInputStream
        extends InputStream
    {
        @Override
        public int read()
            throws IOException
        {
            try
            {
                Thread.sleep( 5 );
            }
            catch ( final InterruptedException e )
            {
            }

            return 0;
        }
    }

    @Rule
    public TestHttpServer server = new TestHttpServer();

    @Test
    public void run()
        throws Exception
    {
        final String repo1 = "repo1";
        final String parentPath = "org/foo/bar";
        final String path = parentPath + "/maven-metadata.xml";

        server.expect( server.formatUrl( repo1, path ), 404, (String) null );
        server.expect( server.formatUrl( repo1, parentPath ), 200, new DelayInputStream() );

        RemoteRepository remote1 = new RemoteRepository( repo1, server.formatUrl( repo1 ) );
        remote1.setMetadata( Location.CONNECTION_TIMEOUT_SECONDS, Integer.toString( 1 ) );

        remote1 = client.stores()
                        .create( remote1, "adding remote", RemoteRepository.class );

        Group g = new Group( "test", remote1.getKey() );
        g = client.stores()
                  .create( g, "adding group", Group.class );

        System.out.printf( "\n\nGroup constituents are:\n  %s\n\n", StringUtils.join( g.getConstituents(), "\n  " ) );

        final InputStream stream = client.content()
                                         .get( group, g.getName(), path );

        // should be 404
        assertThat( stream, nullValue() );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
