/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>A remote repository with accessible remote url</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Client request query by url for remote</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>The remote repository can be got correctly</li>
 * </ul>
 */
public class GetRemoteByUrlTest
        extends AbstractStoreManagementTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    public void getRemoteByUrl()
            throws Exception
    {
        final String repoName = newName();
        final String url = server.formatUrl( repoName );
        final RemoteRepository repo =
                new RemoteRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, repoName, url );
        assertThat( client.stores().create( repo, name.getMethodName(), RemoteRepository.class ), notNullValue() );
        server.expect( url, 200, "" );
        final RemoteRepository remote =
                client.stores().getRemoteByUrl( url, MavenPackageTypeDescriptor.MAVEN_PKG_KEY, RemoteRepository.class );

        assertThat( remote, equalTo( repo ) );
    }

}
