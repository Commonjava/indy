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

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>Repo A and path P</li>
 * </ul>
 *
 * <b>WHEN:</b>
 * <ul>
 *     <li>Retrieve the P from A through Folo trackingId</li>
 *     <li>Seal the trackingId</li>
 *     <li>Get the download records by trackingId</li>
 * </ul>
 *
 * <b>THEN:</b>
 * <ul>
 *     <li>The download record contains path P on store A</li>
 * </ul>
 */
@Category( EventDependent.class )
public class RetrieveFileAndVerifyInTrackingReportTest
    extends AbstractTrackingReportTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    public void run()
        throws Exception
    {
        final String repoId = "repo";
        final String path = "/path/to/foo.class";

        String[] ret = retrieveAndSeal(repoId, path, "This is a test with the same content each time." );

        String trackingId = ret[0];
        String md5 = ret[1];
        String sha256 = ret[2];

        final TrackedContentDTO report = client.module( IndyFoloAdminClientModule.class )
                                               .getTrackingReport( trackingId );
        assertThat( report, notNullValue() );

        final Set<TrackedContentEntryDTO> downloads = report.getDownloads();

        assertThat( downloads, notNullValue() );
        assertThat( downloads.size(), equalTo( 1 ) );

        final TrackedContentEntryDTO entry = downloads.iterator()
                                                    .next();

        System.out.println( entry );

        assertThat( entry, notNullValue() );
        assertThat( entry.getStoreKey(), equalTo( new StoreKey( remote, repoId ) ) );
        assertThat( entry.getPath(), equalTo( path ) );
        assertThat( entry.getLocalUrl(),
                    equalTo( client.content().contentUrl( remote, repoId, path ) ) );
        assertThat( entry.getOriginUrl(), equalTo( server.formatUrl( repoId, path ) ) );
        assertThat( entry.getMd5(), equalTo( md5 ) );
        assertThat( entry.getSha256(), equalTo( sha256 ) );
    }

    protected String[] retrieveAndSeal( String repoId, String path, String content ) throws Exception
    {
        String trackingId = newName();

        final InputStream stream = new ByteArrayInputStream( content.getBytes() );

        server.expect( server.formatUrl( repoId, path ), 200, stream );

        if ( !client.stores().exists( new StoreKey( MAVEN_PKG_KEY, remote, repoId ) ) )
        {
            RemoteRepository rr = new RemoteRepository( MAVEN_PKG_KEY, repoId, server.formatUrl( repoId ) );
            rr = client.stores().create( rr, "adding test remote", RemoteRepository.class );
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final InputStream in = client.module( IndyFoloContentClientModule.class )
                                     .get( trackingId, remote, repoId, path );

        IOUtils.copy( in, baos );
        in.close();

        final byte[] bytes = baos.toByteArray();

        final String md5 = md5Hex( bytes );
        final String sha256 = sha256Hex( bytes );

        assertThat( md5, equalTo( DigestUtils.md5Hex( bytes ) ) );
        assertThat( sha256, equalTo( DigestUtils.sha256Hex( bytes ) ) );

        waitForEventPropagation();

        assertThat( client.module( IndyFoloAdminClientModule.class ).sealTrackingRecord( trackingId ),
                    equalTo( true ) );

        return new String[] { trackingId, md5, sha256 };
    }

    @Override
    protected boolean createStandardStores()
    {
        return false;
    }
}
