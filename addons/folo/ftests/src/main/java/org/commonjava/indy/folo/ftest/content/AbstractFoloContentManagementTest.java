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
package org.commonjava.indy.folo.ftest.content;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.remote;

import java.util.Arrays;
import java.util.Collection;

import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.categories.Category;

@Category( EventDependent.class )
public class AbstractFoloContentManagementTest
    extends AbstractIndyFunctionalTest
{

    protected static final String STORE = "test";

    protected static final String CENTRAL = "central";

    protected static final String PUBLIC = "public";

    @Rule
    public ExpectationServer centralServer = new ExpectationServer();

    @Before
    public void before()
        throws Exception
    {
        final String changelog = "Setup: " + name.getMethodName();
        final HostedRepository hosted =
            this.client.stores()
                       .create( new HostedRepository( STORE ), changelog, HostedRepository.class );

        RemoteRepository central = null;
        if ( client.stores()
                    .exists( remote, CENTRAL ) )
        {
            client.stores().delete( remote, CENTRAL, "removing existing remote:central definition" );
        }

        central =
            client.stores()
                  .create( new RemoteRepository( CENTRAL, centralServer.getBaseUri() ), changelog,
                           RemoteRepository.class );

        Group g;
        if ( client.stores()
                   .exists( group, PUBLIC ) )
        {
            g = client.stores()
                      .load( group, PUBLIC, Group.class );
        }
        else
        {
            g = client.stores()
                      .create( new Group( PUBLIC ), changelog, Group.class );
        }

        g.setConstituents( Arrays.asList( hosted.getKey(), central.getKey() ) );
        client.stores()
              .update( g, changelog );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Arrays.<IndyClientModule> asList( new IndyFoloContentClientModule(), new IndyFoloAdminClientModule() );
    }

}
