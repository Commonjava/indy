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
package org.commonjava.indy.implrepo.maint;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.commonjava.indy.implrepo.client.ImpliedRepoClientModule;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.junit.Test;

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
        final StoreKey impliedKey = new StoreKey( StoreType.remote, IMPLIED );
        final StoreKey testKey = new StoreKey( StoreType.remote, TEST_REPO );

        testRepo =
            client.stores()
                  .create( new RemoteRepository( TEST_REPO, "http://www.bar.com/repo" ), setupChangelog,
                           RemoteRepository.class );

        client.stores()
              .create( new RemoteRepository( IMPLIED, "http://www.foo.com/repo" ), setupChangelog,
                       RemoteRepository.class );

        client.module( ImpliedRepoClientModule.class )
              .setStoresImpliedBy( testRepo, Collections.singletonList( impliedKey ), "setting store implications" );

        pubGroup.addConstituent( testKey );

        client.stores()
              .update( pubGroup, "Add test repo that has implied repos" );

        //        Thread.sleep( 1000 );

        pubGroup = client.stores()
                         .load( StoreType.group, PUBLIC, Group.class );

        assertThat( pubGroup.getConstituents()
                            .contains( impliedKey ), equalTo( true ) );
        System.out.println( "\n\n\n\n\nENDED: " + name.getMethodName() + "\n\n\n\n\n" );
    }

}
