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
