/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.core.live.change;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;

import org.commonjava.aprox.core.change.GroupConsistencyListener;
import org.commonjava.aprox.core.live.AbstractAProxLiveTest;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.Repository;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( Arquillian.class )
public class GroupConsistencyListenerLiveTest
    extends AbstractAProxLiveTest
{

    @Deployment
    public static WebArchive createWar()
    {
        return createWar( GroupConsistencyListenerLiveTest.class );
    }

    @Inject
    private GroupConsistencyListener listener;

    @Test
    public void groupsContainingRepositoryModifiedWhenRepositoryDeleted()
        throws Exception
    {
        final Repository repo = modelFactory.createRepository( "test", "http://repo1.maven.apache.org/maven2/" );
        proxyManager.storeRepository( repo );

        final Group group = modelFactory.createGroup( "testGroup", repo.getKey() );
        proxyManager.storeGroup( group );

        assertThat( group.getConstituents(), notNullValue() );
        assertThat( group.getConstituents()
                         .size(), equalTo( 1 ) );
        assertThat( group.getConstituents()
                         .iterator()
                         .next(), equalTo( repo.getKey() ) );

        Group check = proxyManager.getGroup( group.getName() );

        assertThat( check.getConstituents(), notNullValue() );
        assertThat( check.getConstituents()
                         .size(), equalTo( 1 ) );
        assertThat( check.getConstituents()
                         .iterator()
                         .next(), equalTo( repo.getKey() ) );

        proxyManager.deleteRepository( repo.getName() );

        System.out.println( "Waiting up to 20s for deletions to propagate..." );
        final long start = System.currentTimeMillis();

        listener.waitForChange( 20000, 1000 );

        final long elapsed = System.currentTimeMillis() - start;
        System.out.println( "Continuing test after " + elapsed + " ms." );

        check = proxyManager.getGroup( group.getName() );
        final boolean result = check.getConstituents() == null || check.getConstituents()
                                                                       .isEmpty();

        assertThat( result, equalTo( true ) );
    }

}
