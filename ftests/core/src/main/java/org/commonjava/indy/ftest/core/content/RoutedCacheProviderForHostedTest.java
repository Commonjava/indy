/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
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
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class RoutedCacheProviderForHostedTest
        extends AbstractContentManagementTest
{
    @Test
    public void addHostedAndNFSSetup()
            throws Exception
    {
        final String content = "This is a test: " + System.nanoTime();
        final InputStream stream = new ByteArrayInputStream( content.getBytes() );

        final String path = "/path/to/foo.class";

        final File nfsStorage = new File( fixture.getBootOptions().getIndyHome() + NFS_BASE + "/hosted-" + STORE + path );

        assertThat( client.content().exists( hosted, STORE, path ), equalTo( false ) );
        assertThat( nfsStorage.exists(), equalTo( false ) );

        client.content().store( hosted, STORE, path, stream );

        assertThat( client.content().exists( hosted, STORE, path ), equalTo( true ) );
        assertThat( nfsStorage.exists(), equalTo( true ) );

        final InputStream is = new FileInputStream( nfsStorage );

        final String result = IOUtils.toString( is );
        is.close();

        assertThat( result, equalTo( content ) );

    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        writeConfigFile( "main.conf", readTestResource( "default-test-main.conf" ) );
    }

}
