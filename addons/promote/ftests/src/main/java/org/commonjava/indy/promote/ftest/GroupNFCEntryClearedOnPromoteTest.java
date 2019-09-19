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
package org.commonjava.indy.promote.ftest;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.module.IndyNfcClientModule;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.promote.client.IndyPromoteClientModule;
import org.commonjava.indy.promote.model.GroupPromoteRequest;
import org.commonjava.indy.promote.model.GroupPromoteResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;

/**
 * Check that merged metadata in a group full of hosted repositories is updated when a new hosted repository is added to
 * the membership.
 * <br/>
 * GIVEN:
 * <ul>
 *     <li>HostedRepositories A and B</li>
 *     <li>Group G with HostedRepository member A</li>
 *     <li>HostedRepository A does NOT contain path P</li>
 *     <li>Request for path P in Group G results in 404, NFC entry creation</li>
 * </ul>
 * <br/>
 * WHEN:
 * <ul>
 *     <li>HostedRepository B is appended to the end of Group G's membership by promotion</li>
 *     <li>Path P is requested from Group G <b>after events of membership change have settled</b></li>
 * </ul>
 * <br/>
 * THEN:
 * <ul>
 *     <li>NFC record for Path P should be cleared from Group G</li>
 *     <li>Path P should reflect accurate content from HostedRepository B</li>
 * </ul>
 */
public class GroupNFCEntryClearedOnPromoteTest
        extends AbstractContentManagementTest
{
    private static final String GROUP_G_NAME= "G";
    private static final String HOSTED_A_NAME= "A";
    private static final String HOSTED_B_NAME= "B";

    private static final String PATH = "/org/foo/bar/1/bar-1.pom";

    /* @formatter:off */
    private static final String POM_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<project>\n" +
        "  <modelVersion>4.0.0</modelVersion>\n" +
        "  <groupId>org.foo</groupId>\n" +
        "  <artifactId>bar</artifactId>\n" +
        "  <version>1</version>\n" +
        "  <packaging>pom</packaging>\n" +
        "</project>\n";
    /* @formatter:on */
    private Group g;

    private HostedRepository a;
    private HostedRepository b;

    private final IndyPromoteClientModule promote = new IndyPromoteClientModule();

    @Before
    public void setupRepos()
            throws IndyClientException
    {
        String message = "test setup";

        a = client.stores().create( new HostedRepository( MAVEN_PKG_KEY, HOSTED_A_NAME ), message, HostedRepository.class );
        b = client.stores().create( new HostedRepository( MAVEN_PKG_KEY, HOSTED_B_NAME ), message, HostedRepository.class );

        g = client.stores().create( new Group( MAVEN_PKG_KEY, GROUP_G_NAME, a.getKey() ), message, Group.class );

        client.content()
              .store( b.getKey(), PATH, new ByteArrayInputStream(POM_CONTENT.getBytes()) );
    }

    @Test
    @Category( EventDependent.class )
    public void run()
            throws Exception
    {
        try (InputStream stream = client.content().get( new StoreKey( MAVEN_PKG_KEY, group, GROUP_G_NAME ), PATH ))
        {
            assertThat( "Path should not yet be available from group!", stream,
                        nullValue() );
        }

        // NOTE: This really shouldn't be needed, but I was having trouble getting the NFC entry registered without it.
        try (InputStream stream = client.content().get( new StoreKey( MAVEN_PKG_KEY, group, GROUP_G_NAME ), PATH ))
        {
            assertThat( "NFC: Path should be marked as missing!", stream,
                        nullValue() );
        }

        GroupPromoteRequest request = new GroupPromoteRequest( b.getKey(), g.getName() );
        GroupPromoteResult response = promote.promoteToGroup( request );

        assertThat( response.succeeded(), equalTo( true ) );

        waitForEventPropagation();

        // If you want to see that clearing the NFC works, uncomment this:
//        try (InputStream stream = client.content().get( new StoreKey( MAVEN_PKG_KEY, group, GROUP_G_NAME ), PATH ))
//        {
//            assertThat( "NFC: Path should be marked as missing!", stream,
//                        nullValue() );
//        }
//
//
//        client.module( IndyNfcClientModule.class ).clearAll();

        assertContent( g, PATH, POM_CONTENT );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.singleton( promote );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
