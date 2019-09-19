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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.junit.Test;

import java.util.Set;

public class PullReportWithoutContentAccess404Test
    extends AbstractTrackingReportTest
{

    @Test
    public void run()
        throws Exception
    {
        final String trackingId = newName();

        boolean success = client.module( IndyFoloAdminClientModule.class ).sealTrackingRecord( trackingId );

        assertThat( success, equalTo( true ) );

        final TrackedContentDTO report = client.module( IndyFoloAdminClientModule.class )
                                               .getTrackingReport( trackingId );
        assertThat( report, notNullValue() );

        Set<TrackedContentEntryDTO> downloads = report.getDownloads();
        assertThat( downloads == null || downloads.isEmpty(), equalTo( true ) );

        Set<TrackedContentEntryDTO> uploads = report.getUploads();
        assertThat( uploads == null || uploads.isEmpty(), equalTo( true ) );
    }
}
