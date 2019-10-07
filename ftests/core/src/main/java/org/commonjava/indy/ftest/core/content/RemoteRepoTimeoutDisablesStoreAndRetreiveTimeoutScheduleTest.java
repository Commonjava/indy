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

import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>{@link RemoteRepository} is set with 1s timeout</li>
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
 *     <li>The remote repo will be set to disable when got the error</li>
 *     <li>There will be a schedule timeout in scheduler for this repo</li>
 * </ul>
 */
public class RemoteRepoTimeoutDisablesStoreAndRetreiveTimeoutScheduleTest
        extends AbstractRemoteRepoTimeoutTest
{

    @Override
    protected void initBaseTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        writeConfigFile( "conf.d/indexer.conf", "[indexer]\nenabled=false" );
        writeConfigFile( "conf.d/internal-feature.conf", "[_internal]\nstore.validation.enabled=false" );
    }

    @Test
    public void runTest()
            throws Exception
    {
        super.run();
    }

    @Override
    protected void setRemoteTimeout( RemoteRepository remoteRepo )
    {
        remoteRepo.setTimeoutSeconds( 1 );
    }

    @Override
    protected void assertResult( RemoteRepository remoteRepo )
            throws Exception
    {
        assertThat( remoteRepo.isDisabled(), equalTo( true ) );

        Date timeout = client.schedules().getStoreDisableTimeout( remote, remoteRepo.getName() );
        assertThat( timeout.after( new Date() ), equalTo( true ) );
    }
}
