/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.autoprox.live.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.commonjava.aprox.autoprox.conf.AutoProxConfig;
import org.commonjava.aprox.autoprox.live.fixture.TargetUrlResponder;
import org.commonjava.aprox.autoprox.live.fixture.TestConfigProvider;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.inject.TestData;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.web.json.test.WebFixture;
import org.commonjava.web.test.fixture.TestWarArchiveBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( Arquillian.class )
public class AutoProxDataManagerDecoratorTest
{

    public static final String REPO_ROOT_DIR = "repo.root.dir";

    @Inject
    protected StoreDataManager proxyManager;

    @Inject
    @TestData
    protected AutoProxConfig config;

    @Inject
    protected TargetUrlResponder targetResponder;

    @Rule
    public final WebFixture http = new WebFixture();

    @Before
    public final void setup()
        throws Exception
    {
        proxyManager.install();
        proxyManager.clear();

        RemoteRepository repo = new RemoteRepository( "first", "http://foo.bar/first" );
        proxyManager.storeRemoteRepository( repo );

        repo = new RemoteRepository( "second", "http://foo.bar/second" );
        proxyManager.storeRemoteRepository( repo );
    }

    @After
    public final void teardown()
    {
        targetResponder.clearTargets();
    }

    @Deployment
    public static WebArchive createWar()
    {
        return new TestWarArchiveBuilder( new File( "target/test-assembly.war" ), AutoProxDataManagerDecoratorTest.class ).withExtraClasses( TestConfigProvider.class,
                                                                                                                                             TargetUrlResponder.class )
                                                                                                                          .withLog4jProperties()
                                                                                                                          .withBeansXml( "beans.live.xml" )
                                                                                                                          .build();
    }

    @Test
    public void repositoryAutoCreated()
        throws Exception
    {
        final String testUrl = http.resourceUrl( "target", "test" );
        http.get( testUrl, 404 );
        targetResponder.approveTargets( "test" );
        http.get( testUrl, 200 );

        config.setEnabled( false );
        assertThat( proxyManager.getRemoteRepository( "test" ), nullValue() );
        config.setEnabled( true );

        final RemoteRepository repo = proxyManager.getRemoteRepository( "test" );

        assertThat( repo, notNullValue() );
        assertThat( repo.getName(), equalTo( "test" ) );
        assertThat( repo.getUrl(), equalTo( testUrl ) );

    }

    @Test
    public void groupAutoCreatedWithDeployPointAndTwoRepos()
        throws Exception
    {
        final String testUrl = http.resourceUrl( "target", "test" );
        http.get( testUrl, 404 );
        targetResponder.approveTargets( "test" );
        http.get( testUrl, 200 );

        config.setEnabled( false );
        assertThat( proxyManager.getGroup( "test" ), nullValue() );
        config.setEnabled( true );

        final Group group = proxyManager.getGroup( "test" );

        assertThat( group, notNullValue() );
        assertThat( group.getName(), equalTo( "test" ) );

        final List<StoreKey> constituents = group.getConstituents();

        assertThat( constituents, notNullValue() );
        assertThat( constituents.size(), equalTo( 4 ) );

        int idx = 0;
        StoreKey key = constituents.get( idx );

        assertThat( key.getType(), equalTo( StoreType.hosted ) );
        assertThat( key.getName(), equalTo( "test" ) );

        idx++;
        key = constituents.get( idx );

        assertThat( key.getType(), equalTo( StoreType.remote ) );
        assertThat( key.getName(), equalTo( "test" ) );

        idx++;
        key = constituents.get( idx );

        assertThat( key.getType(), equalTo( StoreType.remote ) );
        assertThat( key.getName(), equalTo( "first" ) );

        idx++;
        key = constituents.get( idx );

        assertThat( key.getType(), equalTo( StoreType.remote ) );
        assertThat( key.getName(), equalTo( "second" ) );
    }

    @Test
    public void repositoryNotAutoCreatedWhenTargetIsInvalid()
        throws Exception
    {
        final String testUrl = http.resourceUrl( "target", "test" );
        http.get( testUrl, 404 );

        config.setEnabled( false );
        assertThat( proxyManager.getRemoteRepository( "test" ), nullValue() );
        config.setEnabled( true );

        final RemoteRepository repo = proxyManager.getRemoteRepository( "test" );

        assertThat( repo, nullValue() );

    }

    @Test
    public void groupNotAutoCreatedWhenTargetIsInvalid()
        throws Exception
    {
        final String testUrl = http.resourceUrl( "target", "test" );
        http.get( testUrl, 404 );

        config.setEnabled( false );
        assertThat( proxyManager.getGroup( "test" ), nullValue() );
        config.setEnabled( true );

        final Group group = proxyManager.getGroup( "test" );

        assertThat( group, nullValue() );
    }

}
