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
package org.commonjava.aprox.ftest.core.content;

import static org.commonjava.aprox.model.core.StoreType.remote;

import java.util.concurrent.CountDownLatch;

import org.commonjava.aprox.ftest.core.fixture.DelayedDownload;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.test.fixture.core.TestHttpServer;
import org.junit.Rule;
import org.junit.Test;

public class ConcurrentMissingMetadataChecksumAndFileDownloadTest
    extends AbstractContentManagementTest
{

    @Rule
    public TestHttpServer server = new TestHttpServer( "repos" );

    @Test
    public void run()
        throws Exception
    {
        final String path = "org/foo/foo-project/maven-metadata.xml";

        final CountDownLatch latch = new CountDownLatch( 2 );

        final DelayedDownload download = new DelayedDownload( client, new StoreKey( remote, STORE ), path, 5, latch );
        newThread( "download", download ).start();

        final DelayedDownload download2 =
            new DelayedDownload( client, new StoreKey( remote, STORE ), path + ".sha1", 1, latch );
        newThread( "download2", download2 ).start();

        System.out.println( "Waiting for content transfers to complete." );
        latch.await();
    }

}
