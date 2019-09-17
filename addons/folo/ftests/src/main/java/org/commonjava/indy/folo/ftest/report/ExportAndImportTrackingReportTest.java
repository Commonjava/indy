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

import org.apache.commons.io.FileUtils;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.commonjava.indy.folo.dto.TrackingIdsDTO;
import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertNull;
import static org.apache.commons.io.IOUtils.copy;
import static org.commonjava.indy.folo.FoloUtils.readZipInputStreamAnd;
import static org.commonjava.indy.folo.data.FoloFiler.FOLO_DIR;
import static org.commonjava.indy.folo.data.FoloFiler.FOLO_SEALED_ZIP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>Three are 2 sealed Folo records</li>
 * </ul>
 *
 * <b>WHEN:</b>
 * <ul>
 *     <li>Export tracking report Zip</li>
 * </ul>
 *
 * <b>THEN:</b>
 * <ul>
 *     <li>The exported zip file contains the 2 sealed records</li>
 * </ul>
 *
 * <b>WHEN:</b>
 * <ul>
 *     <li>Execute clearTrackingRecord to clear all sealed records</li>
 * </ul>
 *
 * <b>THEN:</b>
 * <ul>
 *     <li>All records are cleared</li>
 * </ul>
 *
 * <b>WHEN:</b>
 * <ul>
 *     <li>Execute importTrackingReportZip to import the Zip</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>The 2 records are restored</li>
 * </ul>
 */
@Category( EventDependent.class )
public class ExportAndImportTrackingReportTest
                extends RetrieveFileAndVerifyInTrackingReportTest
{

    final static String repoId = "repo";

    final static String FOLO_TYPE_SEALED = "sealed";

    final static String path1 = "/path/1/foo.class";

    final static String path2 = "/path/2/foo.class";

    @Test
    public void run() throws Exception
    {
        String[] ret = retrieveAndSeal( repoId, path1, "This is a test." );
        String trackingId_1 = ret[0];

        ret = retrieveAndSeal( repoId, path2, "This is a another test." );
        String trackingId_2 = ret[0];

        IndyFoloAdminClientModule adminClientModule = client.module( IndyFoloAdminClientModule.class );
        InputStream stream = adminClientModule.exportTrackingReportZip();

        // check the folo-sealed.zip exists under data/folo/
        File zipFile = new File( dataDir, FOLO_DIR + "/" + FOLO_SEALED_ZIP );
        assertTrue( zipFile.exists() );
        //FileUtils.copyFile(zipFile, new File("/tmp/" + FOLO_SEALED_ZIP));

        // to bytes
        byte[] bytes;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(stream, baos);
        bytes = baos.toByteArray();
        stream.close();

        // read to a list
        final List<TrackedContent> list = new ArrayList<>();
        readZipInputStreamAnd( new ByteArrayInputStream( bytes ), ( record ) -> list.add( record ) );

        // check
        final List<String> trackingIds = check( list );

        assertTrue( trackingIds.contains( trackingId_1 ) );
        assertTrue( trackingIds.contains( trackingId_2 ) );

        // clear all
        trackingIds.forEach( id ->
         {
             try
             {
                 adminClientModule.clearTrackingRecord( id );
             }
             catch ( IndyClientException e )
             {
                 e.printStackTrace();
             }
         } );

        // check ids are cleaned
        TrackingIdsDTO idsDTO = adminClientModule.getTrackingIds( FOLO_TYPE_SEALED );
        assertNull( idsDTO );

        // import
        adminClientModule.importTrackingReportZip( new ByteArrayInputStream( bytes ) );

        // check again
        idsDTO = adminClientModule.getTrackingIds( FOLO_TYPE_SEALED );
        checkIdsDTO( idsDTO, trackingIds, adminClientModule );

    }

    // check the stuff read from the exported stream
    private List<String> check( List<TrackedContent> list )
    {
        assertEquals( 2, list.size() );

        TrackedContent trackedContent_1 = list.get( 0 );
        assertTrue( trackedContent_1.getUploads().isEmpty() );
        Set<TrackedContentEntry> downloads = trackedContent_1.getDownloads();
        assertEquals( 1, downloads.size() );
        System.out.println( ">>>> " + downloads );

        TrackedContent trackedContent_2 = list.get( 1 );
        assertTrue( trackedContent_2.getUploads().isEmpty() );
        downloads = trackedContent_2.getDownloads();
        assertEquals( 1, downloads.size() );
        System.out.println( ">>>> " + downloads );

        List<String> trackingIds = new ArrayList<>();
        list.forEach( trackedContent -> trackingIds.add( trackedContent.getKey().getId() ) );
        return trackingIds;
    }

    static void checkIdsDTO( TrackingIdsDTO idsDTO, List<String> expectedIds, IndyFoloAdminClientModule adminClientModule )
    {
        assertTrue( idsDTO.getSealed().containsAll( expectedIds ) );
        assertEquals( 2, idsDTO.getSealed().size() );

        final List<Exception> ex = new ArrayList<>();
        idsDTO.getSealed().forEach( (id) -> {
            try
            {
                TrackedContentDTO report = adminClientModule.getTrackingReport( id );
                assertNotNull( report );

                System.out.println( ">>>> " + report.getKey() + ", " + report.getDownloads() );

                assertTrue( expectedIds.contains( report.getKey().getId() ));
                assertTrue( !report.getDownloads().isEmpty() );
                assertEquals( 1, report.getDownloads().size() );

                List<TrackedContentEntryDTO> list = new ArrayList( report.getDownloads() );
                TrackedContentEntryDTO entryDTO = list.get( 0 );
                assertTrue( entryDTO.getPath().contains( path1 ) || entryDTO.getPath().contains( path2 ) );
                assertTrue( entryDTO.getStoreKey().getName().equals( repoId ) );
            }
            catch ( IndyClientException e )
            {
                ex.add( e );
            }
        } );
        assertTrue( ex.isEmpty() );
    }
}
