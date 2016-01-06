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
package org.commonjava.indy.ftest.core.content;

import org.apache.http.HttpStatus;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class RemoteRepoTimeoutDisablesStoreAndShowsInDisabledTimeoutsMapTest
    extends AbstractContentManagementTest
{

    public class DelayInputStream
        extends InputStream
    {
        @Override
        public int read()
            throws IOException
        {
            try
            {
                Thread.sleep( 5 );
            }
            catch ( final InterruptedException e )
            {
            }

            return 0;
        }
    }

    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    public void run()
        throws Exception
    {
        final String repo1 = "repo1";
        final String path = "org/foo/bar/maven-metadata.xml";

        server.expect( server.formatUrl( repo1, path ), 200, new DelayInputStream() );

        RemoteRepository remote1 = new RemoteRepository( repo1, server.formatUrl( repo1 ) );
        remote1.setMetadata( Location.CONNECTION_TIMEOUT_SECONDS, Integer.toString( 1 ) );

        remote1 = client.stores()
                        .create( remote1, "adding remote", RemoteRepository.class );

        try
        {
            client.content()
                  .get( remote, repo1, path );
        }
        catch ( final IndyClientException e )
        {
            assertThat( e.getStatusCode(), equalTo( HttpStatus.SC_BAD_GATEWAY ) );
        }

        Thread.sleep( 1000 );

        RemoteRepository result = client.stores().load( remote, repo1, RemoteRepository.class );
        assertThat( result.isDisabled(), equalTo( true ) );

        Map<StoreKey, Date> storeTimeouts = client.schedules().getDisabledStoreTimeouts();
        Date timeout = storeTimeouts.get( new StoreKey( remote, repo1 ) );
        assertThat( timeout, notNullValue() );
        assertThat( timeout.after( new Date() ), equalTo( true ) );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    protected void initBaseTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        writeConfigFile( "conf.d/indexer.conf", "[indexer]\nenabled=false" );
    }
}
