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
package org.commonjava.indy.ftest.core.store;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreType;
import org.junit.Test;

public class AddRemoteRepoThenModifyAndVerifyTest
    extends AbstractStoreManagementTest
{

    @Test
    public void addAndModifyRemoteRepositoryThenRetrieveIt()
        throws Exception
    {
        final RemoteRepository rr = new RemoteRepository( newName(), "http://www.foo.com" );
        client.stores()
              .create( rr, name.getMethodName(), RemoteRepository.class );

        rr.setUrl( "https://www.foo.com/" );

        assertThat( rr.getUrl(), equalTo( "https://www.foo.com/" ) );

        final boolean updated = client.stores()
                                      .update( rr, name.getMethodName() );
        assertThat( updated, equalTo( true ) );

        final RemoteRepository result = client.stores()
                                              .load( StoreType.remote, rr.getName(), RemoteRepository.class );

        assertThat( result.getName(), equalTo( rr.getName() ) );
        assertThat( result.getUrl(), equalTo( rr.getUrl() ) );
        assertThat( result.equals( rr ), equalTo( true ) );
    }
}
