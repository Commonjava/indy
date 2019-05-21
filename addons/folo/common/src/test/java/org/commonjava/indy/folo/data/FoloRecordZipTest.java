package org.commonjava.indy.folo.data;

import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.commonjava.indy.folo.model.StoreEffect;
import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.fail;
import static org.commonjava.indy.folo.FoloUtils.zipTrackedContent;
import static org.commonjava.indy.folo.model.StoreEffect.DOWNLOAD;
import static org.commonjava.indy.folo.model.StoreEffect.UPLOAD;

/**
 * This generates a zip file by a record json file. The zip file can be used to update the broken record, e.g., via
 * curl -X PUT http://<hostname>/api/folo/admin/report/import --data-binary @build-15535.zip
 *
 * To use it, update the keyId, and prepare the /tmp/<keyId>.json, the output file will be in /tmp/<keyId>.zip
 */
public class FoloRecordZipTest
{
    IndyObjectMapper mapper = new IndyObjectMapper( true );

    String keyId = "build-15535"; // example

    @Ignore
    @Test
    public void zipTrackedContentTest() throws IOException
    {
        TrackingKey trackingKey = new TrackingKey( keyId );
        Set<TrackedContentEntry> uploads = new HashSet<>();
        Set<TrackedContentEntry> downloads = new HashSet<>();

        String inputFile = "/tmp/" + keyId + ".json";

        TrackedContentDTO contentDTO = mapper.readValue( new File( inputFile ), TrackedContentDTO.class );
        contentDTO.getDownloads()
                  .forEach( dto -> downloads.add( getTrackedContentEntryByDTO( trackingKey, dto, DOWNLOAD ) ) );
        contentDTO.getUploads()
                  .forEach( dto -> uploads.add( getTrackedContentEntryByDTO( trackingKey, dto, UPLOAD ) ) );

        Set<TrackedContent> sealed = new HashSet<>();

        TrackedContent rec = new TrackedContent( trackingKey, uploads, downloads );
        sealed.add( rec );
        zipTrackedContent( new File( inputFile.replace( ".json", ".zip" ) ), sealed );
    }

    private TrackedContentEntry getTrackedContentEntryByDTO( TrackingKey trackingKey, TrackedContentEntryDTO dto,
                                                             StoreEffect effect )
    {
        return new TrackedContentEntry( trackingKey, dto.getStoreKey(), dto.getAccessChannel(), dto.getOriginUrl(),
                                        dto.getPath(), effect, dto.getSize(), dto.getMd5(), dto.getSha1(),
                                        dto.getSha256() );
    }

    @Ignore
    @Test
    public void nullTrackingKeyTest() throws IOException
    {
        String inputFile = "/tmp/" + keyId + ".json";

        TrackedContent content = mapper.readValue( new File( inputFile ), TrackedContent.class );
        for ( TrackedContentEntry upload : content.getUploads() )
        {
            if ( upload.getTrackingKey() == null )
            {
                System.out.println( "### We got null trackingKey ###" );
                fail();
            }
        }
    }

}
