package org.commonjava.indy.folo.data;

import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.commonjava.indy.folo.FoloUtils.zipTrackedContent;

/**
 * This generates a zip file by a record json file. The zip file can be used to update the broken record, e.g., via
 * curl -X PUT http://<hostname>/api/folo/admin/report/import --data-binary @build-15535.zip
 */
public class FoloRecordZipTest
{
    String inputFile = "/tmp/build-15535.json"; // example

    @Ignore
    @Test
    public void zipTrackedContentTest() throws IOException
    {
        IndyObjectMapper mapper = new IndyObjectMapper( true );
        TrackedContent in = mapper.readValue( new File( inputFile ), TrackedContent.class );
        Set<TrackedContent> sealed = new HashSet<>();
        sealed.add( in );
        zipTrackedContent( new File( inputFile.replace( ".json", ".zip" ) ), sealed );
    }

}
