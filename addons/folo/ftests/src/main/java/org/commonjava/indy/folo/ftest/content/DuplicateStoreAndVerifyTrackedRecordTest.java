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

import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Set;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class DuplicateStoreAndVerifyTrackedRecordTest
                extends AbstractFoloContentManagementTest
{

    final String path = "/path/to/foo.jar";
    final StoreKey storeKey = new StoreKey( MAVEN_PKG_KEY, hosted, STORE );

    @Test
    public void run() throws Exception
    {
        final IndyFoloContentClientModule content = client.module( IndyFoloContentClientModule.class );
        final IndyFoloAdminClientModule admin = client.module( IndyFoloAdminClientModule.class );

        final String trackingId = newName();

        final byte[] b = ( "This is a test: " + System.nanoTime() ).getBytes();
        final InputStream stream = new ByteArrayInputStream( b );
        //System.out.println( ">>> " + b.length ); // 30

        final byte[] b2 = ( "This is another test: " + System.nanoTime() ).getBytes();
        final InputStream stream2 = new ByteArrayInputStream( b2 );
        //System.out.println( ">>> " + b2.length ); // 36

        System.out.println( ">>> store stream" );
        content.store( trackingId, storeKey, path, stream );

        System.out.println( ">>> store stream 2" );
        content.store( trackingId, storeKey, path, stream2 );

        admin.sealTrackingRecord( trackingId );

        TrackedContentDTO report = admin.getTrackingReport( trackingId );
        Set<TrackedContentEntryDTO> uploads = report.getUploads();
        uploads.forEach( et -> {
             System.out.println( ">>> md5: " + et.getMd5() + ", size=" + et.getSize() );
             assertThat( et.getSize(), equalTo( (long) b2.length ) );
        } );
    }

}
