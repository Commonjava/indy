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

import java.util.Arrays;
import java.util.Collection;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_NPM;

@Category( EventDependent.class )
public class AbstractNPMFoloContentManagementTest
        extends AbstractIndyFunctionalTest
{

    protected static final String STORE = "test";

    protected static final String NPMJS = "npmjs";

    protected static final String PUBLIC = "public";

    @Rule
    public ExpectationServer npmjsServer = new ExpectationServer();

    @Before
    public void before()
            throws Exception
    {
        final String changelog = "Setup: " + name.getMethodName();
        final HostedRepository hosted = this.client.stores()
                                                   .create( new HostedRepository( PKG_TYPE_NPM, STORE ), changelog,
                                                            HostedRepository.class );

        RemoteRepository npmjs = null;
        final StoreKey npmjsKey = new StoreKey( PKG_TYPE_NPM, remote, NPMJS );
        if ( client.stores().exists( npmjsKey ) )
        {
            client.stores().delete( npmjsKey, "removing existing remote:npmjs definition" );
        }

        npmjs = client.stores()
                      .create( new RemoteRepository( PKG_TYPE_NPM, NPMJS, npmjsServer.getBaseUri() ), changelog,
                               RemoteRepository.class );

        Group g;
        final StoreKey publicGrpKey = new StoreKey( PKG_TYPE_NPM, group, PUBLIC );
        if ( client.stores().exists( publicGrpKey ) )
        {
            g = client.stores().load( publicGrpKey, Group.class );
        }
        else
        {
            g = client.stores().create( new Group( PKG_TYPE_NPM, PUBLIC ), changelog, Group.class );
        }

        g.setConstituents( Arrays.asList( hosted.getKey(), npmjs.getKey() ) );
        client.stores().update( g, changelog );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Arrays.asList( new IndyFoloContentClientModule(), new IndyFoloAdminClientModule() );
    }

}
