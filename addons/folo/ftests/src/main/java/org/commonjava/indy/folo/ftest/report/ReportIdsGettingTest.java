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

import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.folo.dto.TrackingIdsDTO;
import org.junit.Test;

import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * This case test if the folo tracking ids can be fetched correctly
 * when: <br />
 * <ul>
 *      <li>create central store repo and store a pom</li>
 *      <li>generate the report for central and pom</li>
 *      <li>sealed the report</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>get sealed report ids correctly</li>
 * </ul>
 */
public class ReportIdsGettingTest extends AbstractTrackingReportTest
{
    @Test
    public void doTest() throws Exception{
        final String trackingId = newName();
        final String path = "org/commonjava/commonjava/2/commonjava-2.pom";

        final InputStream result =
                client.module( IndyFoloContentClientModule.class ).get( trackingId, remote, CENTRAL, path );
        assertThat( result, notNullValue() );
        result.close();

        assertThat( client.module( IndyFoloAdminClientModule.class ).initReport( trackingId ), equalTo( true ) );
        assertThat( client.module( IndyFoloAdminClientModule.class ).sealTrackingRecord( trackingId ),
                    equalTo( true ) );

        TrackingIdsDTO ids = client.module( IndyFoloAdminClientModule.class ).getTrackingIds( "sealed" );

        assertNotNull( ids );
        assertNotNull( ids.getSealed() );
        assertThat( ids.getSealed().contains( trackingId ), equalTo( true ) );

        ids = client.module( IndyFoloAdminClientModule.class ).getTrackingIds( "all" );

        assertNotNull( ids );
        assertNotNull( ids.getSealed() );
        assertThat( ids.getSealed().contains( trackingId ), equalTo( true ) );
        assertNull( ids.getInProgress() );
    }
}
