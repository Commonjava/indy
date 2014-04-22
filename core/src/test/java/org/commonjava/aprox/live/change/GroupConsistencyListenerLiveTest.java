/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.live.change;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;

import org.commonjava.aprox.core.change.GroupConsistencyListener;
import org.commonjava.aprox.live.AbstractAProxLiveTest;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.RemoteRepository;
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
        return createWar( GroupConsistencyListenerLiveTest.class ).build();
    }

    @Inject
    private GroupConsistencyListener listener;

    @Test
    public void groupsContainingRepositoryModifiedWhenRepositoryDeleted()
        throws Exception
    {
        final RemoteRepository repo = new RemoteRepository( "test", "http://repo1.maven.apache.org/maven2/" );
        proxyManager.storeRemoteRepository( repo );

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

        proxyManager.deleteRemoteRepository( repo.getName() );

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
