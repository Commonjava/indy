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

import org.commonjava.indy.client.core.module.IndyStoresClientModule;
import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;

import java.io.File;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 */
public abstract class AbstractCacheReportTest
        extends AbstractTrackingReportTest
{
    protected void doRealTest() throws Exception{
        final String trackingId = newName();
        final String path = "org/commonjava/commonjava/2/commonjava-2.pom";

        final InputStream result =
                client.module( IndyFoloContentClientModule.class ).get( trackingId, remote, CENTRAL, path );
        assertThat( result, notNullValue() );
        result.close();

        assertThat( client.module( IndyFoloAdminClientModule.class ).initReport( trackingId ), equalTo( true ) );
        assertThat( client.module( IndyFoloAdminClientModule.class ).sealTrackingRecord( trackingId ),
                    equalTo( true ) );

        final TrackedContentDTO report =
                client.module( IndyFoloAdminClientModule.class ).getTrackingReport( trackingId );
        assertThat( report, notNullValue() );

        final TrackedContentEntryDTO entry = report.getDownloads().iterator().next();

        doDeletion( entry.getStoreKey(), path );

        final String filePath =
                String.format( "%s/var/lib/indy/storage/%s-%s/%s", fixture.getBootOptions().getHomeDir(),
                               remote.name(), CENTRAL, path );
        final File pomFile = new File( filePath );
        assertThat( "File should be deleted", pomFile.exists(), equalTo( false ) );

        final TrackedContentDTO reportAgain =
                client.module( IndyFoloAdminClientModule.class ).getTrackingReport( trackingId );
        assertThat( reportAgain, equalTo( report ) );

        final TrackedContentEntryDTO entryAgain = report.getDownloads().iterator().next();
        assertThat( entryAgain, equalTo( entry ) );
        assertThat( entryAgain.getSha1(), equalTo( entry.getSha1() ) );
        assertThat( entryAgain.getSha256(), equalTo( entry.getSha256() ) );
        assertThat( entryAgain.getMd5(), equalTo( entry.getMd5() ) );
    }

    protected abstract void doDeletion( final StoreKey storeKey, final String path ) throws Exception;
}
