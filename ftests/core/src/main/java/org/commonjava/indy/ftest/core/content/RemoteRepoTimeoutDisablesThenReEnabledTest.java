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

import org.apache.http.HttpStatus;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.ftest.core.category.TimingDependent;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
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

public class RemoteRepoTimeoutDisablesThenReEnabledTest
        extends AbstractContentManagementTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Override
    protected void initBaseTestConfig( CoreServerFixture fixture )
            throws IOException
    {
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        writeConfigFile( "main.conf", "store.disable.timeout=2\n\nInclude conf.d/*.conf\n" );
        writeConfigFile( "conf.d/scheduler.conf", readTestResource( "default-scheduler.conf" ) );
    }

    @Test
    @Category( TimingDependent.class )
    public void run()
        throws Exception
    {
        final String repo1 = "repo1";
        final String path = "org/foo/bar/maven-metadata.xml";

        server.expect( server.formatUrl( "repo1/" ), 200, "OK" );

        server.expect( "GET", server.formatUrl( repo1, path ), (req,resp)->{
            try
            {
                Thread.sleep( 5000 );
            }
            catch ( final InterruptedException e )
            {
            }
            resp.setStatus( 404 );
        } );

        RemoteRepository remote1 = new RemoteRepository( repo1, server.formatUrl( repo1 ) );
        remote1.setTimeoutSeconds( 1 );

        remote1 = client.stores()
                        .create( remote1, "adding remote", RemoteRepository.class );

        try (InputStream is = client.content().get( remote, repo1, path ))
        {
        }
        catch ( final IndyClientException e )
        {
            assertThat( e.getStatusCode(), equalTo( HttpStatus.SC_BAD_GATEWAY ) );
        }

        Thread.sleep( 1000 );

        RemoteRepository result = client.stores().load( remote, repo1, RemoteRepository.class );
        assertThat( result.isDisabled(), equalTo( true ) );

        Thread.sleep( 4000 );

        result = client.stores().load( remote, repo1, RemoteRepository.class );
        assertThat( result.isDisabled(), equalTo( false ) );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
