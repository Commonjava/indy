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
package org.commonjava.indy.implrepo.maint;

import org.commonjava.indy.implrepo.client.ImpliedRepoClientModule;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>RemoteRepositories test and implied</li>
 *     <li>Group pubGroup</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Make remote implied as implied repo for remote test through implied addon client</li>
 *     <li>Add test to pubGroup</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>implied will be added to pubGroup automatically</li>
 * </ul>
 */
public class CreateGroupWithMemberImplicationsTest
    extends AbstractMaintFunctionalTest
{
    private static final String IMPLIED = "implied-repo";

    private static final String TEST_REPO = "test";

    @Test
    public void groupUpdated()
        throws Exception
    {
        System.out.println( "\n\n\n\n\nSTARTING: " + name.getMethodName() + "\n\n\n\n\n" );
        final StoreKey impliedKey = new StoreKey( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, StoreType.remote, IMPLIED );
        final StoreKey testKey = new StoreKey( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, StoreType.remote, TEST_REPO );

        testRepo = client.stores()
                         .create( new RemoteRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, TEST_REPO,
                                                        "http://www.bar.com/repo" ), setupChangelog,
                                  RemoteRepository.class );

        client.stores()
              .create( new RemoteRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, IMPLIED,
                                             "http://www.foo.com/repo" ), setupChangelog, RemoteRepository.class );

        client.module( ImpliedRepoClientModule.class )
              .setStoresImpliedBy( testRepo, Collections.singletonList( impliedKey ), "setting store implications" );

        pubGroup.addConstituent( testKey );

        client.stores().update( pubGroup, "Add test repo that has implied repos" );

        // wait for events...
        Thread.sleep( 2000 );

        pubGroup = client.stores()
                         .load( new StoreKey( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, StoreType.group, PUBLIC ),
                                Group.class );

        assertThat( pubGroup.getConstituents().contains( impliedKey ), equalTo( true ) );
        System.out.println( "\n\n\n\n\nENDED: " + name.getMethodName() + "\n\n\n\n\n" );
    }

}
