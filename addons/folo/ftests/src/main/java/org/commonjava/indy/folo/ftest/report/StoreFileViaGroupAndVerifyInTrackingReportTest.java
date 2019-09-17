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

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Set;

import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category( EventDependent.class )
public class StoreFileViaGroupAndVerifyInTrackingReportTest
    extends AbstractTrackingReportTest
{

    @Test
    public void run()
        throws Exception
    {
        final String trackingId = newName();

        final InputStream stream = new ByteArrayInputStream( ( "This is a test: " + System.nanoTime() ).getBytes() );

        final String path = "/path/to/foo.class";
        client.module( IndyFoloContentClientModule.class )
              .store( trackingId, group, PUBLIC, path, stream );

        assertThat( client.module( IndyFoloAdminClientModule.class ).sealTrackingRecord( trackingId ),
                    equalTo( true ) );

        final TrackedContentDTO report = client.module( IndyFoloAdminClientModule.class )
                                               .getTrackingReport( trackingId );
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
                    equalTo( client.content().contentUrl( hosted, STORE, path ) ) );
        assertThat( entry.getOriginUrl(), nullValue() );
    }
}
