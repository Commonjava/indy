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

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.module.IndyContentClientModule;
import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.HostedRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.util.Set;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@Category( EventDependent.class )
public class StoreFileThenDownloadThenUploadAndVerifyInTrackingReportTest
    extends AbstractTrackingReportTest
{
    final byte[] bytes = ( "This is a test: " + System.nanoTime() ).getBytes();
    final String path = "/path/to/foo.class";

    @Before
    public void prepareStore() throws Exception {
        HostedRepository r = new HostedRepository( STORE );
        r = client.stores().create( r, "adding test hosted", HostedRepository.class );
    }

    @Test
    public void runDownloadThenUpload()
        throws Exception
    {
        final String trackingId = newName();

        // initiate file
        client.module( IndyContentClientModule.class ).store( hosted, STORE, path, new ByteArrayInputStream( bytes ) );

        IndyFoloContentClientModule module = client.module(IndyFoloContentClientModule.class);

        // download
        module.get( trackingId, hosted, STORE, path );

        // upload
        module.store( trackingId, hosted, STORE, path, new ByteArrayInputStream( bytes ) );

        Thread.sleep(2000); // wait for event being fired

        sealAndCheck(trackingId);
    }

    void sealAndCheck(String trackingId) throws IndyClientException {
        // seal
        IndyFoloAdminClientModule adminModule = client.module( IndyFoloAdminClientModule.class );
        boolean success = adminModule.sealTrackingRecord( trackingId );
        assertThat( success, equalTo( true ) );

        // check report
        final TrackedContentDTO report = adminModule.getTrackingReport( trackingId );
        assertThat( report, notNullValue() );

        final Set<TrackedContentEntryDTO> downloads = report.getDownloads();
        assertThat( downloads, notNullValue() );
        assertThat( downloads.size(), equalTo( 1 ) );

        final Set<TrackedContentEntryDTO> uploads = report.getUploads();
        assertThat( uploads, notNullValue() );
        assertThat( uploads.size(), equalTo( 1 ) );
    }
}
