/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Set;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>Repository(remote or group) for npm and path</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Access path through folo track</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>The path can be tracked correctly</li>
 * </ul>
 */
public class VerifyTrackedEntriesForNPMScopedPackageTest
        extends AbstractNPMFoloContentManagementTest
{

    @Test
    public void verifyTrackedEntryForStore() throws Exception
    {

        final String packageContent =
                "{\"name\": \"@babel/code-frame\",\n" + "\"description\": \"Generate errors that contain a code frame that point to source locations.\",\n" + "\"license\": \"MIT\"}";

        final String packagePath = "@babel/code-frame";

        final String trackingId = newName();

        npmjsServer.expect( npmjsServer.formatUrl( packagePath ), 200, new ByteArrayInputStream( packageContent.getBytes() ) );

        IndyFoloContentClientModule folo = client.module( IndyFoloContentClientModule.class );

        final StoreKey storeKey = new StoreKey( NPM_PKG_KEY, group, PUBLIC );

        folo.get( trackingId, storeKey,  packagePath );

        IndyFoloAdminClientModule adminModule = client.module( IndyFoloAdminClientModule.class );
        boolean success = adminModule.sealTrackingRecord( trackingId );
        assertThat( success, equalTo( true ) );

        // check report
        final TrackedContentDTO report = adminModule.getTrackingReport( trackingId );
        assertThat( report, notNullValue() );

        final Set<TrackedContentEntryDTO> downloads = report.getDownloads();
        assertThat( downloads, notNullValue() );
        assertThat( downloads.size(), equalTo( 1 ) );

        downloads.stream().forEach( trackedContentEntryDTO -> {
            assertEquals("/@babel/code-frame", trackedContentEntryDTO.getPath());
        });

    }

}
