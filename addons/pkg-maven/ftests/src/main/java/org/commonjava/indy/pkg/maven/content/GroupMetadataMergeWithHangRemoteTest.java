/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.pkg.maven.content;

import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Test;

import java.io.InputStream;

public class GroupMetadataMergeWithHangRemoteTest
        extends AbstractContentManagementTest
{
    private static final String GROUP_G1_NAME = "G1";

    private static final String GROUP_G2_NAME = "G2";

    private static final String GROUP_G3_NAME = "G3";

    private static final String REMOTE_A1_NAME = "A1";

    private static final String REMOTE_A2_NAME = "A2";

    private static final String REMOTE_A3_NAME = "A3";

    private static final String HOSTED_B_NAME = "B";

    private static final String B_VERSION = "1.0";

    private static final String METADATA_PATH = "/org/foo/bar/maven-metadata.xml";

    private static final String REPO_METADATA_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<metadata>\n" + "  <groupId>org.foo</groupId>\n"
                    + "  <artifactId>bar</artifactId>\n" + "  <versioning>\n" + "    <latest>%version%</latest>\n"
                    + "    <release>%version%</release>\n" + "    <versions>\n" + "      <version>%version%</version>\n"
                    + "    </versions>\n" + "    <lastUpdated>20150722164334</lastUpdated>\n" + "  </versioning>\n"
                    + "</metadata>\n";
    /* @formatter:on */

    private static final String GROUP_METADATA_CONTENT =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<metadata>\n" + "  <groupId>org.foo</groupId>\n"
                    + "  <artifactId>bar</artifactId>\n" + "  <versioning>\n" + "    <latest>1.0</latest>\n"
                    + "    <release>1.0</release>\n" + "    <versions>\n" + "      <version>1.0</version>\n"
                    + "    </versions>\n" + "    <lastUpdated>20150722164334</lastUpdated>\n" + "  </versioning>\n"
                    + "</metadata>\n";
    /* @formatter:on */

    @Test
    public void remoteWith408Response()
            throws Exception
    {
        String message = "test metadata merge";

        RemoteRepository remote = new RemoteRepository( REMOTE_A1_NAME, server.formatUrl( REMOTE_A1_NAME ) );
        remote.setTimeoutSeconds( 600 );
        RemoteRepository a = client.stores().create( remote, message, RemoteRepository.class );
        server.expect( "GET", server.formatUrl( REMOTE_A1_NAME, METADATA_PATH ), 408, "Request Time-out" );

        HostedRepository b =
                client.stores().create( new HostedRepository( HOSTED_B_NAME ), message, HostedRepository.class );
        deployContent( b, METADATA_PATH, REPO_METADATA_TEMPLATE, B_VERSION );

        Group g = client.stores().create( new Group( GROUP_G1_NAME, a.getKey(), b.getKey() ), message, Group.class );

        assertContent( g, METADATA_PATH, GROUP_METADATA_CONTENT );
    }

    @Test
    public void remoteWithUpstreamTimeout()
            throws Exception
    {
        String message = "test metadata merge";

        RemoteRepository remote = new RemoteRepository( REMOTE_A2_NAME, server.formatUrl( REMOTE_A2_NAME ) );
        remote.setTimeoutSeconds( 2 );

        RemoteRepository a = client.stores().create( remote, message, RemoteRepository.class );
        server.expect( "GET", server.formatUrl( REMOTE_A2_NAME, METADATA_PATH ), 200,
                       new DelayInputStream( 1000 * 5 ) );

        HostedRepository b =
                client.stores().create( new HostedRepository( HOSTED_B_NAME ), message, HostedRepository.class );
        deployContent( b, METADATA_PATH, REPO_METADATA_TEMPLATE, B_VERSION );

        Group g = client.stores().create( new Group( GROUP_G2_NAME, a.getKey(), b.getKey() ), message, Group.class );

        assertContent( g, METADATA_PATH, GROUP_METADATA_CONTENT );
    }

    //    @Test
    // This is used to test the hanging remote so need time cost, also need to enlarge the indy client RequestTimeoutSeconds
    public void remoteWithSlowResponse()
            throws Exception
    {
        String message = "test metadata merge";

        RemoteRepository remote = new RemoteRepository( REMOTE_A3_NAME, server.formatUrl( REMOTE_A3_NAME ) );
        remote.setTimeoutSeconds( 600 );

        RemoteRepository a = client.stores().create( remote, message, RemoteRepository.class );
        server.expect( "GET", server.formatUrl( REMOTE_A3_NAME, METADATA_PATH ), 200,
                       new DelayInputStream( 1000 * 60 * 3 ) );

        HostedRepository b =
                client.stores().create( new HostedRepository( HOSTED_B_NAME ), message, HostedRepository.class );
        deployContent( b, METADATA_PATH, REPO_METADATA_TEMPLATE, B_VERSION );

        Group g = client.stores().create( new Group( GROUP_G3_NAME, a.getKey(), b.getKey() ), message, Group.class );

        assertContent( g, METADATA_PATH, GROUP_METADATA_CONTENT );
    }

    public class DelayInputStream
            extends InputStream
    {
        private final long transferTime;

        public DelayInputStream( long transferTime )
        {
            this.transferTime = transferTime;
        }

        @Override
        public int read()
        {
            try
            {
                Thread.sleep( transferTime );
            }
            catch ( final InterruptedException e )
            {
            }

            return -1;
        }
    }
}
