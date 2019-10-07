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
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.maven.galley.model.Location;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>{@link RemoteRepository} is set with "connection-timeout" with 1s</li>
 *     <li>{@link RemoteRepository} is not set with any disable timeout</li>
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
public class RemoteRepoTimeoutDisablesStoreAndShowsInDisabledTimeoutsMapTest
        extends AbstractRemoteRepoTimeoutTest
{

    @Test
    public void runTest()
            throws Exception
    {
        super.run();
    }

    @Override
    protected void initBaseTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        writeConfigFile( "conf.d/indexer.conf", "[indexer]\nenabled=false" );
        writeConfigFile( "conf.d/internal-features.conf", "[_internal]\nstore.validation.enabled=false" );

    }

    @Override
    protected void setRemoteTimeout( RemoteRepository remoteRepo )
    {
        remoteRepo.setMetadata( Location.CONNECTION_TIMEOUT_SECONDS, Integer.toString( 1 ) );
    }

    @Override
    protected void assertResult( RemoteRepository remoteRepo )
            throws Exception
    {
        assertThat( remoteRepo.isDisabled(), equalTo( true ) );

        Map<StoreKey, Date> storeTimeouts = client.schedules().getDisabledStoreTimeouts();
        Date timeout = storeTimeouts.get( new StoreKey( remote, remoteRepo.getName() ) );
        assertThat( timeout, notNullValue() );
        assertThat( timeout.after( new Date() ), equalTo( true ) );
    }
}
