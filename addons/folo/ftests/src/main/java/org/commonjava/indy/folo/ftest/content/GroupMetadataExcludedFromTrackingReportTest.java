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
package org.commonjava.indy.folo.ftest.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.junit.Test;

import java.io.InputStream;
import java.util.Set;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class GroupMetadataExcludedFromTrackingReportTest
    extends AbstractFoloContentManagementTest
{

    @Test
    public void run()
        throws Exception
    {
        final String trackingId = newName();

        final String path = "org/commonjava/commonjava/maven-metadata.xml";
        centralServer.expect( centralServer.formatUrl( path ), 200, Thread.currentThread()
                                                                          .getContextClassLoader()
                                                                          .getResourceAsStream(
                                                                                  "folo-content/commonjava-version-metadata.xml" ) );

        client.module( IndyFoloAdminClientModule.class ).initReport( trackingId );
        final InputStream result = client.module( IndyFoloContentClientModule.class )
                                         .get( trackingId, group, PUBLIC, path );

        assertThat( result, notNullValue() );

        final String pom = IOUtils.toString( result );
        result.close();
        assertThat( pom.contains( "<version>2</version>" ), equalTo( true ) );

        boolean sealed = client.module( IndyFoloAdminClientModule.class ).sealTrackingRecord( trackingId );
        assertThat( sealed, equalTo( true ) );

        TrackedContentDTO trackingReport =
                client.module( IndyFoloAdminClientModule.class ).getTrackingReport( trackingId );

        Set<TrackedContentEntryDTO> downloads = trackingReport.getDownloads();
        assertThat( downloads == null || downloads.isEmpty(), equalTo( true ) );
    }

}
