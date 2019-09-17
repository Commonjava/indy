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
package org.commonjava.indy.folo.model;

import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.commonjava.indy.folo.model.StoreEffect.DOWNLOAD;
import static org.commonjava.indy.model.core.AccessChannel.GENERIC_PROXY;
import static org.commonjava.indy.model.core.AccessChannel.MAVEN_REPO;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_GENERIC_HTTP;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TrackedContentEntryTest
{
    /**
     * We have to hack this test a bit in order to test the ability to deserialize this first version of
     * TrackedContentEntry.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void externalizeAsV1_readCurrentVersion()
            throws IOException, ClassNotFoundException
    {
        TrackedContentEntryV1 ev1 = new TrackedContentEntryV1( new TrackingKey( "test-key" ),
                                                               new StoreKey( PKG_TYPE_GENERIC_HTTP, remote,
                                                                             "some-upstream" ), GENERIC_PROXY,
                                                               "http://some.upstream.url/path/to/file", "path/to/file",
                                                               DOWNLOAD, 10101010L, "aaaafffffccccceeeeddd",
                                                               "bbbcccceeeedddaaaaa", "aaadadaaaadaeee" );

        TrackedContentEntry out = new TrackedContentEntry( new TrackingKey( "test-key2" ),
                                                               new StoreKey( PKG_TYPE_MAVEN, hosted,
                                                                             "some-upstream2" ), MAVEN_REPO,
                                                               "http://some.upstream.url/path/to/file2", "path/to/file2",
                                                               DOWNLOAD, 10101011L, "aaaafffffccccceeeedddfffffff",
                                                               "bbbcccceeeedddaaaaaffffffff", "aaadadaaaadaeeeffffffff" );

        TrackedContentEntry test =
                new TrackedContentEntry( ev1.getTrackingKey(), ev1.getStoreKey(), ev1.getAccessChannel(),
                                         ev1.getOriginUrl(), ev1.getPath(), ev1.getEffect(), ev1.getSize(),
                                         ev1.getMd5(), ev1.getSha1(), ev1.getSha256() );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        ev1.writeExternal( oos );
        oos.flush();

        ObjectInputStream oin = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );
        out.readExternal( oin );

        assertThat( out.getTrackingKey(), equalTo( test.getTrackingKey() ) );
        assertThat( out.getStoreKey(), equalTo( test.getStoreKey() ) );
        assertThat( out.getAccessChannel(), equalTo( test.getAccessChannel() ) );
        assertThat( out.getOriginUrl(), equalTo( test.getOriginUrl() ) );
        assertThat( out.getPath(), equalTo( test.getPath() ) );
        assertThat( out.getEffect(), equalTo( test.getEffect() ) );
        assertThat( out.getSize(), equalTo( test.getSize() ) );
        assertThat( out.getMd5(), equalTo( test.getMd5() ) );
        assertThat( out.getSha1(), equalTo( test.getSha1() ) );
        assertThat( out.getSha256(), equalTo( test.getSha256() ) );
    }

    @Test
    public void serializeRoundTrip_CurrentVersion()
            throws IOException, ClassNotFoundException
    {
        TrackedContentEntry entry = new TrackedContentEntry( new TrackingKey( "test-key" ),
                                                               new StoreKey( PKG_TYPE_GENERIC_HTTP, remote,
                                                                             "some-upstream" ), GENERIC_PROXY,
                                                               "http://some.upstream.url/path/to/file", "path/to/file",
                                                               DOWNLOAD, 10101010L, "aaaafffffccccceeeeddd",
                                                               "bbbcccceeeedddaaaaa", "aaadadaaaadaeee" );

        TrackedContentEntry test =
                new TrackedContentEntry( entry.getTrackingKey(), entry.getStoreKey(), entry.getAccessChannel(),
                                         entry.getOriginUrl(), entry.getPath(), entry.getEffect(), entry.getSize(),
                                         entry.getMd5(), entry.getSha1(), entry.getSha256() );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( entry );
        oos.flush();

        ObjectInputStream oin = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );
        TrackedContentEntry out = (TrackedContentEntry) oin.readObject();

        assertThat( out.getTrackingKey(), equalTo( test.getTrackingKey() ) );
        assertThat( out.getStoreKey(), equalTo( test.getStoreKey() ) );
        assertThat( out.getAccessChannel(), equalTo( test.getAccessChannel() ) );
        assertThat( out.getOriginUrl(), equalTo( test.getOriginUrl() ) );
        assertThat( out.getPath(), equalTo( test.getPath() ) );
        assertThat( out.getEffect(), equalTo( test.getEffect() ) );
        assertThat( out.getSize(), equalTo( test.getSize() ) );
        assertThat( out.getMd5(), equalTo( test.getMd5() ) );
        assertThat( out.getSha1(), equalTo( test.getSha1() ) );
        assertThat( out.getSha256(), equalTo( test.getSha256() ) );
    }
}
