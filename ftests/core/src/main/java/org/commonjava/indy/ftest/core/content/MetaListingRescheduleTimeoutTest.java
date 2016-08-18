/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.ftest.core.content;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.client.core.helper.PathInfo;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Test if the special ".listing.txt" metadata content re-schedule mechanism is working. When metadata is re-requested during the timeout interval,
 * the timeout for this content should NOT be re-scheduled.
 */
public class MetaListingRescheduleTimeoutTest
        extends AbstractContentManagementTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    @Test
    @Category( EventDependent.class )
    public void timeout()
            throws Exception
    {
        final int METADATA_TIMEOUT_SECONDS = 4;
        final int METADATA_TIMEOUT_WAITING_MILLISECONDS = 3000;

        final String repoId = "test-repo";
        // path without trailing slash for ExpectationServer...
        String repoRootPath = "org/foo/bar";
        final String repoRootUrl = server.formatUrl( repoId, repoRootPath );
        // now append the trailing '/' so Indy knows to try a directory listing...
        repoRootPath += "/";
        final String repoSubPath1 = "org/foo/bar/1.0/pom.xml";
        final String repoSubPath2 = "org/foo/bar/1.1/pom.xml";

        final String repoSubUrl1 = server.formatUrl( repoId, repoSubPath1 );
        final String repoSubUrl2 = server.formatUrl( repoId, repoSubPath2 );

        // mocking up a http server that expects access to metadata
        final String listingContent = "<html>" + "<head><title>Index of /org/foo/bar</title></head>"
                + "<body><h1>Index of /org/foo/bar/</h1>"
                + "<hr><pre>"
                + "<a href=\"1.0/\">1.0/</a>"
                + "<a href=\"1.1/\">1.1/</a>"
                + "</pre><hr></body></html>";
        server.expect( repoRootUrl, 200, listingContent );
        final String datetime = ( new Date() ).toString();
        server.expect( repoSubUrl1, 200, String.format( "metadata %s", datetime ) );
        server.expect( repoSubUrl2, 200, String.format( "metadata %s", datetime ) );

        // set up remote repository pointing to the test http server, and timeout little later
        final String changelog = "Timeout Testing: " + name.getMethodName();
        final RemoteRepository repository = new RemoteRepository( repoId, server.formatUrl( repoId ) );
        repository.setMetadataTimeoutSeconds( METADATA_TIMEOUT_SECONDS );
        client.stores().create( repository, changelog, RemoteRepository.class );

        client.content().get( remote, repoId, repoSubPath1 );
        client.content().get( remote, repoId, repoSubPath2 );

        // first time trigger normal content storage with timeout, should be 4s
        InputStream content = client.content().get( remote, repoId, repoRootPath );
        assertThat( "no metadata result", content, notNullValue() );
        logger.debug("### will begin to get content");
        IOUtils.readLines( content ).forEach( logger::info );

        final String listingMetaPath = "org/foo/bar/.listing.txt";
        String listingMetaFilePath =
                String.format( "%s/var/lib/indy/storage/%s-%s/%s", fixture.getBootOptions().getIndyHome(),
                               remote.name(), repoId, listingMetaPath );
        File listingMetaFile = new File( listingMetaFilePath );
        assertThat( "metadata doesn't exist", listingMetaFile.exists(), equalTo( true ) );

        // wait for first 2.5s
        Thread.sleep( METADATA_TIMEOUT_WAITING_MILLISECONDS );

        // as the metadata content re-request, the metadata timeout interval should NOT be re-scheduled
        client.content().get( remote, repoId, repoRootPath );

        // will wait another 2.5s
        Thread.sleep( METADATA_TIMEOUT_WAITING_MILLISECONDS );

//        logger.info( "Checking whether metadata file {} has been deleted...", listingMetaFile );
        // as rescheduled, the artifact should not be deleted
        assertThat( "artifact should be removed as the rescheduled of metadata should not succeed",
                    listingMetaFile.exists(), equalTo( false ) );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
