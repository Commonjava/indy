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
package org.commonjava.indy.folo.ftest.report;

import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Set;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@Category( EventDependent.class )
public class StoreFileAndRecalculateTrackingEntryTest
    extends AbstractTrackingReportTest
{

    @Test
    public void run()
        throws Exception
    {
        final String trackingId = newName();

        byte[] oldData = ( "This is a test: " + System.nanoTime() ).getBytes();
        InputStream stream = new ByteArrayInputStream( oldData );

        final String path = "/path/to/foo.class";
        client.module( IndyFoloContentClientModule.class )
              .store( trackingId, hosted, STORE, path, stream );

        IndyFoloAdminClientModule adminModule = client.module( IndyFoloAdminClientModule.class );
        boolean success = adminModule.sealTrackingRecord( trackingId );
        assertThat( success, equalTo( true ) );

        TrackedContentDTO report = adminModule
                                               .getTrackingReport( trackingId );
        assertThat( report, notNullValue() );

        Set<TrackedContentEntryDTO> uploads = report.getUploads();

        assertThat( uploads, notNullValue() );
        assertThat( uploads.size(), equalTo( 1 ) );

        TrackedContentEntryDTO entry = uploads.iterator()
                                                    .next();

        System.out.println( entry );

        assertThat( entry, notNullValue() );
        assertThat( entry.getStoreKey(), equalTo( new StoreKey( hosted, STORE ) ) );
        assertThat( entry.getPath(), equalTo( path ) );
        assertThat( entry.getLocalUrl(),
                    equalTo( client.content().contentUrl( hosted, STORE, path ) ) );
        assertThat( entry.getMd5(), equalTo( md5Hex( oldData ) ) );
        assertThat( entry.getOriginUrl(), nullValue() );

        byte[] newData = ( "This is a REPLACED test: " + System.nanoTime() ).getBytes();
        stream = new ByteArrayInputStream( newData );

        client.content()
              .store( hosted, STORE, path, stream );

        report = adminModule
                .recalculateTrackingRecord( trackingId );

        assertThat( report, notNullValue() );

        uploads = report.getUploads();

        assertThat( uploads, notNullValue() );
        assertThat( uploads.size(), equalTo( 1 ) );

        entry = uploads.iterator().next();

        System.out.println( entry );

        assertThat( entry, notNullValue() );
        assertThat( entry.getStoreKey(), equalTo( new StoreKey( hosted, STORE ) ) );
        assertThat( entry.getPath(), equalTo( path ) );
        assertThat( entry.getLocalUrl(),
                    equalTo( client.content().contentUrl( hosted, STORE, path ) ) );
        assertThat( entry.getMd5(), equalTo( md5Hex( newData ) ) );
        assertThat( entry.getOriginUrl(), nullValue() );

    }
}
