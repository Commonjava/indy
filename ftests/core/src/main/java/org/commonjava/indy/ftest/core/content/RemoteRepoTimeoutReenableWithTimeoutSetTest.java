/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
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

import org.apache.http.HttpStatus;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.core.change.StoreEnablementManager;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.ftest.core.category.TimingDependent;
import org.commonjava.indy.ftest.core.store.AbstractStoreManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class RemoteRepoTimeoutReenableWithTimeoutSetTest
        extends AbstractRemoteRepoTimeoutTest
{

    @Category( TimingDependent.class )
    @Test
    public void runTest()
            throws Exception
    {
        super.run();
    }

    @Override
    protected void setRemoteTimeout( RemoteRepository remoteRepo )
    {
        remoteRepo.setMetadata( Location.CONNECTION_TIMEOUT_SECONDS, Integer.toString( 1 ) );
        remoteRepo.setDisableTimeout( 2 );
    }

    @Override
    protected void assertResult( RemoteRepository remoteRepo )
            throws Exception
    {
        assertThat( remoteRepo.isDisabled(), equalTo( true ) );

        Thread.sleep( 2000 );

        RemoteRepository result = client.stores().load( remote, remoteRepo.getName(), RemoteRepository.class );
        assertThat( result.isDisabled(), equalTo( false ) );
    }
}
