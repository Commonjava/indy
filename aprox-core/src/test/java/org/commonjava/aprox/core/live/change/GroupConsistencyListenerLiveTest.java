/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.core.live.change;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;

import org.commonjava.aprox.core.change.GroupConsistencyListener;
import org.commonjava.aprox.core.fixture.ProxyConfigProvider;
import org.commonjava.aprox.core.live.AbstractAProxLiveTest;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.web.test.fixture.TestWarArchiveBuilder;
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
        return new TestWarArchiveBuilder( GroupConsistencyListenerLiveTest.class ).withExtraClasses( AbstractAProxLiveTest.class,
                                                                                                     ProxyConfigProvider.class )
                                                                                  .build();
    }

    @Inject
    private GroupConsistencyListener listener;

    @Test
    public void groupsContainingRepositoryModifiedWhenRepositoryDeleted()
        throws Exception
    {
        final Repository repo = new Repository( "test", "http://repo1.maven.apache.org/maven2/" );
        proxyManager.storeRepository( repo );

        final Group group = new Group( "testGroup", repo.getKey() );
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
