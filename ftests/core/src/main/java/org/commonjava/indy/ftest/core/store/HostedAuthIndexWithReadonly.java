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
package org.commonjava.indy.ftest.core.store;

import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>A hosted repo</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Change the hosted repo to readonly</li>
 *     <li>Change the hosted repo back to non-readonly</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>The hosted repo will also set to authoritative index on when changed to readonly</li>
 *     <li>The hosted repo will set to authoritative index off when changed back to non-readonly</li>
 * </ul>
 */
public class HostedAuthIndexWithReadonly
        extends AbstractStoreManagementTest
{
    @Test
    @Category( EventDependent.class )
    public void addAndModifyHostedReadonlyThenAuthIndex()
            throws Exception
    {
        final String repoName = newName();
        HostedRepository repo = new HostedRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, repoName );
        repo = client.stores()
                     .create( repo, name.getMethodName(), HostedRepository.class );

        assertThat( repo.isReadonly(), equalTo( false ) );
        assertThat( repo.isAuthoritativeIndex(), equalTo( false ));

        repo.setReadonly( true );
        assertThat( client.stores()
                          .update( repo, name.getMethodName() ), equalTo( true ) );

        Thread.sleep( 500 );

        repo = client.stores()
                     .load( new StoreKey( MAVEN_PKG_KEY, StoreType.hosted, repo.getName() ), HostedRepository.class );
        assertThat( repo.getName(), equalTo( repo.getName() ) );
        assertThat( repo.isReadonly(), equalTo( true ) );
        assertThat( repo.isAuthoritativeIndex(), equalTo( true ) );

        repo.setReadonly( false );
        assertThat( client.stores()
                          .update( repo, name.getMethodName() ), equalTo( true ) );

        Thread.sleep( 500 );

        repo = client.stores()
                     .load( new StoreKey( MAVEN_PKG_KEY, StoreType.hosted, repo.getName() ), HostedRepository.class );
        assertThat( repo.getName(), equalTo( repo.getName() ) );
        assertThat( repo.isReadonly(), equalTo( false ) );
        assertThat( repo.isAuthoritativeIndex(), equalTo( false ) );
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestConfig( fixture );
        writeConfigFile( "conf.d/content-index.conf", "[content-index]\nsupport.authoritative.indexes=true"  );
    }
}
