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

import org.commonjava.indy.client.core.helper.PathInfo;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.ftest.core.fixture.DelayedDownload;
import org.commonjava.indy.ftest.core.fixture.InputTimer;
import org.commonjava.indy.ftest.core.fixture.ReluctantInputStream;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DownloadMetadataBeforeTargetTransferCompleteTest
        extends AbstractContentManagementTest
{

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    @Test
    public void run()
        throws Exception
    {
        RemoteRepository rmt = new RemoteRepository( STORE, server.formatUrl( STORE ) );
        rmt.setTimeoutSeconds( 30 );

        client.stores()
              .create( rmt, "adding test proxy",
                       RemoteRepository.class );

        final String path = "org/foo/foo-project/1/foo-1.txt";
        final String metaPath = path + ".http-metadata.json";

        final byte[] data = ( "This is a test: " + System.nanoTime() ).getBytes();

        final CountDownLatch latch = new CountDownLatch( 3 );

        final ReluctantInputStream stream = new ReluctantInputStream( data );
        server.expect( server.formatUrl( STORE, path ), 200, stream );

        final InputTimer input = new InputTimer( stream, 10000 / data.length, latch );
        newThread( "input", input ).start();

        final DelayedDownload download = new DelayedDownload( client, new StoreKey( remote, STORE ), path, 0, latch );
        newThread( "download", download ).start();

        final DelayedDownload meta = new DelayedDownload( client, new StoreKey( remote, STORE ), metaPath, 1000, latch );
        newThread( "download-meta", meta ).start();

        System.out.println( "Waiting for content transfers to complete." );
        latch.await();

        logger.info( ">>>> Input started: {}, ended: {} \nDownload started: {}, ended: {} \nMeta started: {}, ended: {}\n",
                input.getStartTime(), input.getEndTime(),
                download.getStartTime(), download.getEndTime(),
                meta.getStartTime(), meta.getEndTime() );

        // Assert download data
        assertThat( download.getContent().toByteArray(), equalTo( data ) );

        // Assert meta download starts after target, and finishes before target
        assertThat( meta.getStartTime() > download.getStartTime(), equalTo( true ) );
        assertThat( meta.getEndTime() < download.getEndTime(), equalTo( true ) );

        // Assert meta exists
        final PathInfo result = client.content().getInfo( remote, STORE, metaPath );
        assertThat( "no result", result, notNullValue() );
        assertThat( "doesn't exist", result.exists(), equalTo( true ) );
    }

}
