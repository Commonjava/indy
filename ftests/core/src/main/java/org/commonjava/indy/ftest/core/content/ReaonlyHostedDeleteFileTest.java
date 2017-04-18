/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.ftest.core.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.util.ApplicationStatus;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ReaonlyHostedDeleteFileTest
        extends AbstractContentManagementTest
{

    @Test
    public void deleteFileNotAllowed()
        throws Exception
    {
        final String content = "This is a test: " + System.nanoTime();
        InputStream stream = new ByteArrayInputStream( content.getBytes() );

        final String path = "/path/to/foo.class";

        final String repoName = "test-hosted";
        HostedRepository repo = new HostedRepository( repoName );
        repo = client.stores().create( repo, name.getMethodName(), HostedRepository.class );

        assertThat( client.content()
                          .exists( hosted, repoName, path ), equalTo( false ) );

        client.content()
              .store( hosted, repoName, path, stream );

        assertThat( client.content()
                          .exists( hosted, repoName, path ), equalTo( true ) );

        repo.setReadonly( true );
        client.stores().update( repo, name.getMethodName() );

        try
        {
            client.content()
                  .delete( hosted, repoName, path );
        }
        catch ( IndyClientException e )
        {
            assertThat( e.getStatusCode(), equalTo( ApplicationStatus.METHOD_NOT_ALLOWED.code() ) );
        }

        assertThat( client.content()
                          .exists( hosted, repoName, path ), equalTo( true ) );

        repo.setReadonly( false );
        client.stores().update( repo, name.getMethodName() );

        client.content()
              .delete( hosted, repoName, path );

        assertThat( client.content()
                          .exists( hosted, repoName, path ), equalTo( false ) );

    }
}
