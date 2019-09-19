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
package org.commonjava.indy.pkg.npm.content;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.util.ApplicationStatus;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * This case tests if files can be deleted in a readonly hosted repo
 * when: <br />
 * <ul>
 *      <li>creates a non-readonly hosted repo and stores file in it</li>
 *      <li>updates the hosted repo to non-readonly</li>
 *      <li>deletes the file in hosted repo once</li>
 *      <li>updates the hosted repo to non-readonly</li>
 *      <li>deletes file again</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the file can not be deleted with 405 error first time</li>
 *     <li>the file can be deleted successfully with no error second time</li>
 * </ul>
 */
public class NPMReaonlyHostedDeleteFileTest
                extends AbstractContentManagementTest
{

    @Test
    public void test() throws Exception
    {
        final String content = "This is a test: " + System.nanoTime();
        InputStream stream = new ByteArrayInputStream( content.getBytes() );

        final String path = "jquery/2.1.0";

        final String repoName = "test-npm-hosted";
        HostedRepository repo = new HostedRepository( NPM_PKG_KEY, repoName );
        repo = client.stores().create( repo, "adding npm hosted repo", HostedRepository.class );

        StoreKey storeKey = repo.getKey();
        assertThat( client.content().exists( storeKey, path ), equalTo( false ) );

        client.content().store( storeKey, path, stream );

        assertThat( client.content().exists( storeKey, path ), equalTo( true ) );

        repo.setReadonly( true );
        client.stores().update( repo, "change read-only true" );

        try
        {
            client.content().delete( storeKey, path );
        }
        catch ( IndyClientException e )
        {
            assertThat( e.getStatusCode(), equalTo( ApplicationStatus.METHOD_NOT_ALLOWED.code() ) );
        }

        assertThat( client.content().exists( storeKey, path ), equalTo( true ) );

        repo.setReadonly( false );
        client.stores().update( repo, "change read-only false" );

        client.content().delete( storeKey, path );

        assertThat( client.content().exists( storeKey, path ), equalTo( false ) );

        stream.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
