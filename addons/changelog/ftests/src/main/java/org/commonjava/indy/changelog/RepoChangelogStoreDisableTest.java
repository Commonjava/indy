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
import org.commonjava.indy.changelog.client.IndyRepoChangelogClientModule;
import org.commonjava.indy.client.core.IndyClientException;
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

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * This test ensure that change log module will not work if disabled.
 * GIVEN: <br />
 * <ul>
 *     <li>A hosted repo</li>
 * </ul>
 * WHEN: <br />
 * <ul>
 *      <li>Module disabled from configuration</li>
 *      <li>Update/creates to this repo</li>
 * </ul>
 * THEN: <br />
 * <ul>
 *     <li>No change log entries when searching from this repo</li>
 *     <li>No change log entries when when searching all</li>
 * </ul>
 */
public class RepoChangelogStoreDisableTest
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

        IndyRepoChangelogClientModule repoChangelogClientModule = client.module( IndyRepoChangelogClientModule.class );

        List<ChangeEvent> logs = null;
        try
        {
            logs = repoChangelogClientModule.getByStoreKey( repo.getKey() );
        }
        catch ( IndyClientException e )
        {
            assertThat( e.getStatusCode(), equalTo( 404 ) );
        }

        assertNotNull( logs );
        assertTrue( logs.isEmpty() );

        try
        {
            repoChangelogClientModule.getAll();
        }
        catch ( IndyClientException e )
        {
            assertThat( e.getStatusCode(), equalTo( 404 ) );
        }

        assertNotNull( logs );
        assertTrue( logs.isEmpty() );
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
        writeConfigFile( "conf.d/repo-changelog.conf", "[repo-changelog]\nenabled=false" );
    }
}
