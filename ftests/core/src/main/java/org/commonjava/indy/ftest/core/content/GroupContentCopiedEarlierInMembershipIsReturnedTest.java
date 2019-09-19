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
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 1/10/17.
 *
 * <b>GIVEN:</b>
 * <ul>
 *     <li>{@link org.commonjava.indy.model.core.Group} A contains {@link org.commonjava.indy.model.core.HostedRepository} X</li>
 *     <li>{@link org.commonjava.indy.model.core.Group} B contains {@link org.commonjava.indy.model.core.HostedRepository} Y in first membership slot</li>
 *     <li>{@link org.commonjava.indy.model.core.Group} B contains {@link org.commonjava.indy.model.core.Group} A in second membership slot</li>
 *     <li>Content #1 is deployed to path P in {@link org.commonjava.indy.model.core.HostedRepository} X</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Content #2 is deployed to path P in {@link org.commonjava.indy.model.core.HostedRepository} Y</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>Resolving path P from {@link org.commonjava.indy.model.core.Group} B returns Content #2.</li>
 * </ul>
 */
public class GroupContentCopiedEarlierInMembershipIsReturnedTest
        extends AbstractContentManagementTest
{

    private static final String REPO_X = "X";

    private static final String REPO_Y = "Y";

    private static final String GROUP_A = "A";

    private static final String GROUP_B = "B";

    private static final String CONTENT_1 = "This is content #1.";

    private static final String CONTENT_2 = "This is content #2.";

    private static final String PATH = "/path/to/test.txt";

    private HostedRepository repoX;

    private HostedRepository repoY;

    private Group groupA;

    private Group groupB;

    private byte[] content2;

    @Before
    public void setupStores()
            throws IndyClientException, UnsupportedEncodingException
    {
        String changelog = "test setup";
        repoX = client.stores().create( new HostedRepository( REPO_X ), changelog, HostedRepository.class );
        repoY = client.stores().create( new HostedRepository( REPO_Y ), changelog, HostedRepository.class );

        groupA = client.stores().create( new Group( GROUP_A, repoX.getKey() ), changelog, Group.class );
        groupB = client.stores().create( new Group( GROUP_B, repoY.getKey(), groupA.getKey() ), changelog, Group.class );

        client.content().store( repoX.getKey(), PATH, new ByteArrayInputStream( CONTENT_1.getBytes( "UTF-8" ) ) );

        content2 = CONTENT_2.getBytes( "UTF-8" );
    }

    @Test
    public void run()
            throws IndyClientException, IOException
    {
        assertContent( repoX, PATH, CONTENT_1 );
        assertContent( groupA, PATH, CONTENT_1 );
        assertContent( groupB, PATH, CONTENT_1 );

        // now, copy the content into hosted repo Y.
        client.content().store( repoY.getKey(), PATH, new ByteArrayInputStream( content2 ) );

        // the content should be available in repoY.
        assertContent( repoY, PATH, CONTENT_2 );

        // the content should also be available in groupB.
        assertContent( groupB, PATH, CONTENT_2 );
    }

}
