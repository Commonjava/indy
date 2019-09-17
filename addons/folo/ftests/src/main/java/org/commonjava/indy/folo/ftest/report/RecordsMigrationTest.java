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
import org.commonjava.indy.folo.dto.TrackingIdsDTO;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.commonjava.indy.folo.ctl.FoloConstants.TRACKING_TYPE.SEALED;
import static org.commonjava.indy.folo.data.FoloFiler.FOLO_DIR;
import static org.commonjava.indy.folo.data.FoloFiler.FOLO_SEALED_ZIP;
import static org.commonjava.indy.folo.ftest.report.ExportAndImportTrackingReportTest.checkIdsDTO;
import static org.junit.Assert.assertNotNull;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>There is a folo-sealed.zip file under indy/data/folo folder</li>
 * </ul>
 *
 * <b>WHEN:</b>
 * <ul>
 *     <li>Indy starts</li>
 * </ul>
 *
 * <b>THEN:</b>
 * <ul>
 *     <li>The records in the zip file are imported to sealed</li>
 *     <li>The folo-sealed.dat is backed up to folo-sealed.dat.bak</li>
 *     <li>The folo-sealed.zip is rename to folo-sealed.zip.loaded after migration is done</li>
 * </ul>
 */
public class RecordsMigrationTest
                extends AbstractTrackingReportTest
{
    @Override
    protected void initTestData( CoreServerFixture fixture ) throws IOException
    {
        //copy folo-sealed.zip to data/folo/
        copyToDataFile( "migration/" + FOLO_SEALED_ZIP, FOLO_DIR + "/" + FOLO_SEALED_ZIP );
    }

    @Test
    public void run() throws Exception
    {
        IndyFoloAdminClientModule adminClientModule = client.module( IndyFoloAdminClientModule.class );

        TrackingIdsDTO idsDTO = adminClientModule.getTrackingIds( SEALED.getValue() );
        assertNotNull( idsDTO );

        List<String> expectedIds = Arrays.asList( "Mg4NV207", "qC8c1cZB" );
        checkIdsDTO( idsDTO, expectedIds, adminClientModule );
    }
}
