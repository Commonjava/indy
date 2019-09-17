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
package org.commonjava.indy.changelog;

import org.commonjava.auditquery.history.ChangeEvent;
import org.commonjava.auditquery.history.ChangeType;
import org.commonjava.indy.changelog.client.IndyRepoChangelogClientModule;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * This test ensure that each repo change will trigger a change log entry in cache.
 * GIVEN: <br />
 * <ul>
 *     <li>A hosted repo</li>
 * </ul>
 * WHEN: <br />
 * <ul>
 *      <li>Create this test repo use indy store api</li>
 *      <li>Update this repo twice</li>
 * </ul>
 * THEN: <br />
 * <ul>
 *     <li>The UPDATE changelog entries for this repo should be 2 through searching from this repo</li>
 *     <li>The CREATE changelog entries for this repo should be 1 through searching from this repo</li>
 *     <li>The all changelog entries for this repo should be 3 through searching all</li>
 *     <li>The UPDATE changelog entries should be 2 through searching all</li>
 * </ul>
 */
public class RepoChangelogStoreTest
        extends AbstractIndyFunctionalTest
{

    @Test
    public void test()
            throws Exception
    {
        HostedRepository repo = new HostedRepository( MAVEN_PKG_KEY, newName() );
        final StoreKey hostedKey = repo.getKey();
        repo = client.stores().create( repo, name.getMethodName(), HostedRepository.class );
        repo.setAllowReleases( !repo.isAllowReleases() );
        client.stores().update( repo, name.getMethodName() );
        repo.setReadonly( true );
        client.stores().update( repo, name.getMethodName() );

        List<ChangeEvent> logs =
                client.module( IndyRepoChangelogClientModule.class ).getByStoreKey( repo.getKey() );

        final AtomicInteger createCount = new AtomicInteger( 0 );
        final AtomicInteger updateCount = new AtomicInteger( 0 );
        logs.forEach( c -> {
            assertThat( c.getStoreKey(), equalTo( hostedKey.toString() ) );
            if ( c.getChangeType() == ChangeType.CREATE )
            {
                createCount.getAndIncrement();
            }
            if ( c.getChangeType() == ChangeType.UPDATE )
            {
                updateCount.getAndIncrement();
            }
        } );

        assertThat( createCount.get(), equalTo( 1 ) );
        assertThat( updateCount.get(), equalTo( 2 ) );

        logs = client.module( IndyRepoChangelogClientModule.class ).getAll();

        final AtomicInteger updateCount2 = new AtomicInteger( 0 );
        final AtomicInteger testRepoCount = new AtomicInteger( 0 );

        logs.forEach( c -> {
            if ( c.getChangeType() == ChangeType.UPDATE )
            {
                updateCount2.getAndIncrement();
            }
            if ( c.getStoreKey().equals( hostedKey.toString() ) )
            {
                testRepoCount.getAndIncrement();
            }
        } );

        assertThat( updateCount2.get(), equalTo( 2 ) );
        assertThat( testRepoCount.get(), equalTo( 3 ) );

    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.singletonList( new IndyRepoChangelogClientModule() );
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestConfig( fixture );
        writeConfigFile( "conf.d/repo-changelog.conf", "[repo-changelog]\nenabled=true" );
    }
}
