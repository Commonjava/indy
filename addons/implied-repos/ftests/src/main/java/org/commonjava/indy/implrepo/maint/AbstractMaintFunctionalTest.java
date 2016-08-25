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
package org.commonjava.indy.implrepo.maint;

import static org.commonjava.indy.model.core.StoreType.group;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.implrepo.client.ImpliedRepoClientModule;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractMaintFunctionalTest
    extends AbstractIndyFunctionalTest
{

    protected static final String PUBLIC = "public";

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    protected RemoteRepository testRepo;

    protected Group pubGroup;

    protected String setupChangelog;

    @Before
    public void setupTestStores()
        throws Exception
    {
        setupChangelog = "Create test structures";

        if ( client.stores()
                   .exists( group, PUBLIC ) )
        {
            System.out.println( "Loading pre-existing public group." );
            pubGroup = client.stores()
                      .load( group, PUBLIC, Group.class );
        }
        else
        {
            System.out.println( "Creating new group 'public'" );
            pubGroup = client.stores()
                             .create( new Group( PUBLIC ), setupChangelog, Group.class );
        }
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        writeConfigFile( "conf.d/implied-repos.conf", "[implied-repos]\nenabled=true\nenabled.group=public" );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.<IndyClientModule> singleton( new ImpliedRepoClientModule() );
    }
}
