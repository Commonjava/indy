/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.autoprox.ftest.del;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.junit.Test;

public class DeleteGroupMemberTest
    extends AbstractAutoproxDeletionTest
{

    @Test
    public void run()
        throws Exception
    {
        final String named = "test";

        expectRepoAutoCreation( named );

        final RemoteRepository r = client.stores()
                                         .load( StoreType.remote, named, RemoteRepository.class );

        assertThat( r, notNullValue() );

        Group group = new Group( "group", r.getKey() );
        group = client.stores()
                      .create( group, "Adding test group", Group.class );

        client.stores()
              .delete( StoreType.remote, named, "Removing test repo" );

        System.out.println( "Waiting for server events to clear..." );
        synchronized ( this )
        {
            wait( 3000 );
        }

        final StoreListingDTO<RemoteRepository> remotes = client.stores()
                                                                .listRemoteRepositories();

        boolean found = false;
        for ( final RemoteRepository remote : remotes )
        {
            if ( remote.getName()
                       .equals( named ) )
            {
                found = true;
                break;
            }
        }

        assertThat( found, equalTo( false ) );

        group = client.stores()
                      .load( StoreType.group, group.getName(), Group.class );
        assertThat( group.getConstituents()
                         .isEmpty(), equalTo( true ) );
    }

}
