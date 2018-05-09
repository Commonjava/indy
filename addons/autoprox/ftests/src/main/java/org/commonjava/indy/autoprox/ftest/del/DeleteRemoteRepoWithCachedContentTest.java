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
import static org.junit.Assert.assertThat;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DeleteRemoteRepoWithCachedContentTest
    extends AbstractAutoproxDeletionTest
{

    @Test
    @Category( EventDependent.class )
    public void deleteRepoWithContent_RepoNotReCreatedWhenContentIsDeleted()
        throws Exception
    {
        final String named = "test";
        final String path = "path/to/foo.txt";
        final String content = "This is a test";

        expectRepoAutoCreation( named );
        http.expect( http.formatUrl( named, path ), 200, content );

        final InputStream stream = client.content()
                                         .get( StoreType.remote, named, path );

        final String retrieved = IOUtils.toString( stream );
        stream.close();

        assertThat( retrieved, equalTo( content ) );

        System.out.println( "Waiting for server events to clear..." );
        waitForEventPropagation();

        client.stores()
              .delete( StoreType.remote, named, "Removing test repo" );

        System.out.println( "Waiting for server events to clear..." );
        waitForEventPropagation();

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
    }

}
