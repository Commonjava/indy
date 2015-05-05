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
package org.commonjava.aprox.folo.ftest.report;

import static org.commonjava.aprox.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.client.core.util.UrlUtils;
import org.commonjava.aprox.folo.client.AproxFoloAdminClientModule;
import org.commonjava.aprox.folo.client.AproxFoloContentClientModule;
import org.commonjava.aprox.folo.dto.TrackedContentDTO;
import org.commonjava.aprox.folo.dto.TrackedContentEntryDTO;
import org.commonjava.aprox.model.core.StoreKey;
import org.junit.Test;

public class StoreFileAndVerifyInTrackingReportTest
    extends AbstractTrackingReportTest
{

    @Test
    public void storeFileAndVerifyInReport()
        throws Exception
    {
        final String trackingId = newName();

        final InputStream stream = new ByteArrayInputStream( ( "This is a test: " + System.nanoTime() ).getBytes() );

        final String path = "/path/to/foo.class";
        client.module( AproxFoloContentClientModule.class )
              .store( trackingId, hosted, STORE, path, stream );

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final InputStream in = client.module( AproxFoloContentClientModule.class )
                                     .get( trackingId, hosted, STORE, path );

        IOUtils.copy( in, baos );
        in.close();

        final byte[] bytes = baos.toByteArray();

        final String md5 = md5Hex( bytes );
        final String sha256 = sha256Hex( bytes );

        assertThat( md5, equalTo( DigestUtils.md5Hex( bytes ) ) );
        assertThat( sha256, equalTo( DigestUtils.sha256Hex( bytes ) ) );

        final TrackedContentDTO report = client.module( AproxFoloAdminClientModule.class )
                                               .getTrackingReport( trackingId, hosted, STORE );
        assertThat( report, notNullValue() );

        final Set<TrackedContentEntryDTO> uploads = report.getUploads();

        assertThat( uploads, notNullValue() );
        assertThat( uploads.size(), equalTo( 1 ) );

        final TrackedContentEntryDTO entry = uploads.iterator()
                                                    .next();

        System.out.println( entry );

        assertThat( entry, notNullValue() );
        assertThat( entry.getStoreKey(), equalTo( new StoreKey( hosted, STORE ) ) );
        assertThat( entry.getPath(), equalTo( path ) );
        assertThat( entry.getLocalUrl(),
                    equalTo( UrlUtils.buildUrl( client.getBaseUrl(), hosted.singularEndpointName(), STORE, path ) ) );
        assertThat( entry.getOriginUrl(), nullValue() );
        assertThat( entry.getMd5(), equalTo( md5 ) );
        assertThat( entry.getSha256(), equalTo( sha256 ) );
    }
}
