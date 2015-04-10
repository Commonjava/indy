package org.commonjava.aprox.folo.ftest.urls;

import static org.commonjava.aprox.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.util.Set;

import org.commonjava.aprox.folo.dto.TrackedContentDTO;
import org.commonjava.aprox.folo.dto.TrackedContentEntryDTO;
import org.junit.Test;

public class StoreOneTrackedAndVerifyUrlInReportTest
    extends AbstractFoloUrlsTest
{

    @Test
    public void storeOneFileAndVerifyItInParentDirectoryListing()
        throws Exception
    {
        final byte[] data = "this is a test".getBytes();
        final ByteArrayInputStream stream = new ByteArrayInputStream( data );
        final String root = "/path/to/";
        final String path = root + "foo.txt";

        final String trackingId = "tracker";

        content.store( trackingId, hosted, STORE, path, stream );

        final TrackedContentDTO report = admin.getTrackingReport( trackingId, hosted, STORE );

        final Set<TrackedContentEntryDTO> uploads = report.getUploads();
        for ( final TrackedContentEntryDTO upload : uploads )
        {
            final String uploadPath = upload.getPath();
            final String localUrl = client.content()
                                          .contentUrl( hosted, STORE, uploadPath );

            assertThat( "Incorrect local URL for upload: '" + uploadPath + "'", upload.getLocalUrl(),
                        equalTo( localUrl ) );
        }
    }

}
