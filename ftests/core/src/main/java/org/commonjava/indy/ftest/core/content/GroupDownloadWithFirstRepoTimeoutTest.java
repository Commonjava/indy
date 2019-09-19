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
import org.commonjava.maven.galley.model.Location;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class GroupDownloadWithFirstRepoTimeoutTest
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
        final String path = "org/foo/bar/test.txt";

        /* @formatter:off */
        final String repo2Content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<metadata>\n" + 
            "  <groupId>org.foo</groupId>\n" + 
            "  <artifactId>bar</artifactId>\n" + 
            "  <versioning>\n" + 
            "    <latest>1.0</latest>\n" + 
            "    <release>1.0</release>\n" + 
            "    <versions>\n" + 
            "      <version>1.0</version>\n" + 
            "    </versions>\n" + 
            "    <lastUpdated>20150722164334</lastUpdated>\n" + 
            "  </versioning>\n" + 
            "</metadata>\n";
        /* @formatter:on */

        server.expect( "GET", server.formatUrl( repo1, path ), (request, response)->{
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.info( "DELAY 5s..." );
            try
            {
                Thread.sleep( 5000 );
            }
            catch ( final InterruptedException e )
            {
            }

            logger.info( "...WAKE and return" );
            response.setStatus( 404 );
        } );

        server.expect( server.formatUrl( repo2, path ), 200, repo2Content );

        RemoteRepository remote1 = new RemoteRepository( repo1, server.formatUrl( repo1 ) );
        remote1.setTimeoutSeconds( 2 );

        remote1 = client.stores()
                        .create( remote1, "adding remote", RemoteRepository.class );

        RemoteRepository remote2 = new RemoteRepository( repo2, server.formatUrl( repo2 ) );
        remote2.setMetadata( Location.CONNECTION_TIMEOUT_SECONDS, Integer.toString( 1 ) );

        remote2 = client.stores()
                        .create( remote2, "adding remote", RemoteRepository.class );

        Group g = new Group( "test", remote1.getKey(), remote2.getKey() );
        g = client.stores()
                  .create( g, "adding group", Group.class );

        System.out.printf( "\n\nGroup constituents are:\n  %s\n\n", StringUtils.join( g.getConstituents(), "\n  " ) );

        final InputStream stream = client.content()
                                         .get( group, g.getName(), path );

        assertThat( stream, notNullValue() );

        final String metadata = IOUtils.toString( stream );
        assertThat( metadata, equalTo( repo2Content ) );
        stream.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
