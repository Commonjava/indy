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

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.remote;

import java.util.Arrays;

import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.TestHttpServer;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

public class AbstractContentManagementTest
    extends AbstractIndyFunctionalTest
{

    protected static final String STORE = "test";

    protected static final String CENTRAL = "central";

    protected static final String PUBLIC = "public";

    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    protected Thread newThread( final String named, final Runnable runnable )
    {
        final Thread t = new Thread( runnable );
        t.setDaemon( true );
        t.setName( name.getMethodName() + " :: " + named );

        return t;
    }

    @Before
    public void before()
        throws Exception
    {
        if ( !createStandardTestStructures() )
        {
            return;
        }

        final String changelog = "Create test structures";

        final HostedRepository hosted =
            this.client.stores()
                       .create( new HostedRepository( STORE ), changelog, HostedRepository.class );

        client.stores()
              .delete( remote, CENTRAL, "removing to setup test instance" );

        final RemoteRepository central =
            client.stores()
                      .create( new RemoteRepository( CENTRAL, server.formatUrl( "central" ) ), changelog,
                               RemoteRepository.class );

        Group g;
        if ( client.stores()
                   .exists( group, PUBLIC ) )
        {
            System.out.println( "Loading pre-existing public group." );
            g = client.stores()
                      .load( group, PUBLIC, Group.class );
        }
        else
        {
            System.out.println( "Creating new group 'public'" );
            g = client.stores()
                      .create( new Group( PUBLIC ), changelog, Group.class );
        }

        g.setConstituents( Arrays.asList( hosted.getKey(), central.getKey() ) );
        client.stores()
              .update( g, changelog );
    }

    protected boolean createStandardTestStructures()
    {
        return true;
    }

}
