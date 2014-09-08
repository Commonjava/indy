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
package org.commonjava.aprox.core.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.http.auth.BasicUserPrincipal;
import org.commonjava.aprox.audit.SecuritySystem;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.RemoteRepository;
import org.junit.Before;
import org.junit.Test;

public abstract class RepositoryDataManagerTCK
    extends AbstractProxyDataManagerTCK
{

    private Principal principal;

    private StoreDataManager manager;

    private SecuritySystem security;

    @Before
    public void setup()
        throws Exception
    {
        doSetup();
        seedRepositoriesForGroupTests();
    }

    protected void doSetup()
        throws Exception
    {
    }

    protected void seedRepositoriesForGroupTests()
        throws Exception
    {
        principal = new BasicUserPrincipal( "test-user" );

        manager = getFixtureProvider().getDataManager();
        security = getFixtureProvider().getSecuritySystem();
    }

    @Test
    public void createAndRetrieveCentralRepoProxy()
        throws Exception
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        storeRemoteRepository( repo, false );

        final RemoteRepository result = manager.getRemoteRepository( repo.getName() );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.getUrl(), equalTo( repo.getUrl() ) );
        assertThat( result.getUser(), nullValue() );
        assertThat( result.getPassword(), nullValue() );
    }

    @Test
    public void createCentralRepoProxyTwiceAndRetrieveOne()
        throws Exception
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        storeRemoteRepository( repo, true );

        storeRemoteRepository( repo, true );

        final List<? extends RemoteRepository> result = manager.getAllRemoteRepositories();

        assertThat( result, notNullValue() );
        assertThat( result.size(), equalTo( 1 ) );
    }

    @Test
    public void createAndDeleteCentralRepoProxy_ByName()
        throws Exception
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        storeRemoteRepository( repo, false );

        final ProxyDataException e = security.runAs( principal, new PrivilegedAction<ProxyDataException>()
        {
            @Override
            public ProxyDataException run()
            {
                try
                {
                    manager.deleteRemoteRepository( repo.getName(), "test" );
                }
                catch ( final ProxyDataException e )
                {
                    return e;
                }

                return null;
            }
        } );

        if ( e != null )
        {
            throw e;
        }

        final ArtifactStore result = manager.getRemoteRepository( repo.getName() );

        assertThat( result, nullValue() );
    }

    @Test
    public void createAndDeleteCentralRepoProxy_ByObject()
        throws Exception
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        storeRemoteRepository( repo, false );

        final ProxyDataException e = security.runAs( principal, new PrivilegedAction<ProxyDataException>()
        {
            @Override
            public ProxyDataException run()
            {
                try
                {
                    manager.deleteRemoteRepository( repo, "test" );
                }
                catch ( final ProxyDataException e )
                {
                    return e;
                }

                return null;
            }
        } );

        if ( e != null )
        {
            throw e;
        }

        final ArtifactStore result = manager.getRemoteRepository( repo.getName() );

        assertThat( result, nullValue() );
    }

    @Test
    public void createTwoReposAndRetrieveAll()
        throws Exception
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        storeRemoteRepository( repo );

        final RemoteRepository repo2 = new RemoteRepository( "test", "http://www.google.com" );
        storeRemoteRepository( repo2 );

        final List<? extends RemoteRepository> repositories = manager.getAllRemoteRepositories();

        assertThat( repositories, notNullValue() );
        assertThat( repositories.size(), equalTo( 2 ) );

        Collections.sort( repositories, new Comparator<RemoteRepository>()
        {

            @Override
            public int compare( final RemoteRepository r1, final RemoteRepository r2 )
            {
                return r1.getName()
                         .compareTo( r2.getName() );
            }
        } );

        ArtifactStore r = repositories.get( 0 );
        assertThat( r.getName(), equalTo( repo.getName() ) );

        r = repositories.get( 1 );
        assertThat( r.getName(), equalTo( repo2.getName() ) );
    }

    private void storeRemoteRepository( final RemoteRepository repo )
        throws Exception
    {
        final ProxyDataException e = security.runAs( principal, new PrivilegedAction<ProxyDataException>()
        {

            @Override
            public ProxyDataException run()
            {
                try
                {
                    manager.storeArtifactStore( repo, "test" );
                }
                catch ( final ProxyDataException e )
                {
                    return e;
                }

                return null;
            }
        } );

        if ( e != null )
        {
            throw e;
        }
    }

    private void storeRemoteRepository( final RemoteRepository repo, final boolean skipIfExists )
        throws Exception
    {
        final ProxyDataException e = security.runAs( principal, new PrivilegedAction<ProxyDataException>()
        {

            @Override
            public ProxyDataException run()
            {
                try
                {
                    manager.storeArtifactStore( repo, "test", skipIfExists );
                }
                catch ( final ProxyDataException e )
                {
                    return e;
                }

                return null;
            }
        } );

        if ( e != null )
        {
            throw e;
        }
    }

}
