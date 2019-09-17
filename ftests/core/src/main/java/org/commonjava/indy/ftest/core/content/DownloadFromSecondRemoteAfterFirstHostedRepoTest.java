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
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 1/31/17.
 */
public class DownloadFromSecondRemoteAfterFirstHostedRepoTest
        extends AbstractContentManagementTest
{
    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    private static final String REMOTE = "remote";

    private static final String HOSTED = "hosted";

    private static final String GROUP = "group";

    private static final String PATH = "org/foo/bar/1/bar-1.pom";

    private static final String JAR_PATH = "org/foo/bar/1/bar-1.jar";

    private static final String CONTENT =
            "<project><modelVersion>4.0.0</modelVersion><groupId>org.foo</groupId><artifactId>bar</artifactId><version>1</version></project>";

    private RemoteRepository remote;

    private HostedRepository hosted;

    private Group group;

    @Before
    public void setupTest()
            throws Exception
    {
        String change = "test setup";
        hosted = client.stores().create( new HostedRepository( HOSTED ), change, HostedRepository.class );
        remote = client.stores()
                       .create( new RemoteRepository( REMOTE, server.formatUrl( REMOTE ) ), change,
                                RemoteRepository.class );

        group = client.stores().create( new Group( GROUP, hosted.getKey(), remote.getKey() ), change, Group.class );

        client.content().store( hosted.getKey(), JAR_PATH, new ByteArrayInputStream( "This is the jar".getBytes() ) );

        server.expect( server.formatUrl( REMOTE, PATH ), 200, CONTENT );
    }

    @Test
    public void run()
            throws IndyClientException, IOException
    {
        boolean exists = client.content().exists( group.getKey(), PATH );
        assertThat( exists, equalTo( true ) );

        try(InputStream inputStream = client.content().get( group.getKey(), PATH ))
        {
            assertThat( inputStream, notNullValue() );

            String content = IOUtils.toString( inputStream );
            assertThat( content, equalTo( CONTENT ) );
        }
    }
}
