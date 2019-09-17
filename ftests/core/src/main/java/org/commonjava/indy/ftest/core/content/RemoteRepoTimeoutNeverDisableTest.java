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

import org.commonjava.indy.core.change.StoreEnablementManager;
import org.commonjava.indy.ftest.core.category.TimingDependent;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.maven.galley.model.Location;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>{@link RemoteRepository} is set with "connection-timeout" with 1s</li>
 *     <li>{@link RemoteRepository} is set with -1 of disable timeout</li>
 *     <li>The remote proxy gives a connection timeout error for repo</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Request repo for artifact and got timeout error</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>The remote repo will never be set to disable</li>
 * </ul>
 */
public class RemoteRepoTimeoutNeverDisableTest
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
        remoteRepo.setDisableTimeout( StoreEnablementManager.TIMEOUT_NEVER_DISABLE );
    }

    @Override
    protected void assertResult( RemoteRepository remoteRepo )
            throws Exception
    {
        assertThat( remoteRepo.isDisabled(), equalTo( false ) );

        Thread.sleep( 2000 );

        RemoteRepository result = client.stores().load( remote, remoteRepo.getName(), RemoteRepository.class );
        assertThat( result.isDisabled(), equalTo( false ) );
    }

}
