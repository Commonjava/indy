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

import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
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
 *     <li>Two remote repositories with same accessible remote url</li>
 *     <li>One remote repo with another remote url</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Client request query by url for the first two remote</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>These two remote repositories can be got correctly</li>
 *     <li>The third one will not be got</li>
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
        final String urlName = "urltest";
        final String url = server.formatUrl( urlName );
        final RemoteRepository remote1 =
                new RemoteRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, newName(), url );
        assertThat( client.stores().create( remote1, name.getMethodName(), RemoteRepository.class ), notNullValue() );

        final RemoteRepository remote2 =  new RemoteRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, newName(), url );
        assertThat( client.stores().create( remote2, name.getMethodName(), RemoteRepository.class ), notNullValue() );

        final RemoteRepository remote3 = new RemoteRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, newName(), server.formatUrl("another test") );
        assertThat( client.stores().create( remote2, name.getMethodName(), RemoteRepository.class ), notNullValue() );

        server.expect( url, 200, "" );
        final StoreListingDTO<RemoteRepository> remotes =
                client.stores().getRemoteByUrl( url, MavenPackageTypeDescriptor.MAVEN_PKG_KEY );

        assertThat( remotes, notNullValue() );
        assertThat( remotes.getItems().contains( remote1 ), equalTo( true ));
        assertThat( remotes.getItems().contains( remote2 ), equalTo( true ));
        assertThat( remotes.getItems().contains( remote3 ), equalTo( false ));
    }

}
