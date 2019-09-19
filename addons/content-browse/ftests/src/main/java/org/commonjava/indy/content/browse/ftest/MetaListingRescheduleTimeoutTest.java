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
package org.commonjava.indy.content.browse.ftest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.content.browse.client.IndyContentBrowseClientModule;
import org.commonjava.indy.content.browse.model.ContentBrowseResult;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.ftest.core.category.TimingDependent;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * This case test if the ".listing.txt" metadata re-schedule is working.
 * when: <br />
 * <ul>
 *      <li>creates a remote repo with two pom artifacts and a ".listing.txt" source</li>
 *      <li>set metadata timeout for remote</li>
 *      <li>request the content after a short period during the schedule</li>
 *      <li>set remote.list.download.enabled to true to make remote listing download enabled</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the metadata schedule should not be reset which let the .listing wait for long time to be deleted</li>
 * </ul>
 */
public class MetaListingRescheduleTimeoutTest
        extends AbstractContentManagementTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer(  );

    //    @Ignore // content listing is disabled for now, 2018/4/3
    @Test
    @Category( TimingDependent.class )
    public void timeout()
            throws Exception
    {
        final int METADATA_TIMEOUT_SECONDS = 4;
        final int METADATA_TIMEOUT_WAITING_MILLISECONDS = 3000;

        final String repoId = "test-repo";
        String repoRootPath = "org/foo/bar/";
        final String repoRootUrl = server.formatUrl( repoId, repoRootPath );
        // now append the trailing '/' so Indy knows to try a directory listing...
        final String repoSubPath1 = "org/foo/bar/1.0/pom.xml";
        final String repoSubPath2 = "org/foo/bar/1.1/pom.xml";

        final String repoSubUrl1 = server.formatUrl( repoId, repoSubPath1 );
        final String repoSubUrl2 = server.formatUrl( repoId, repoSubPath2 );

        // mocking up a http server that expects access to metadata
        final String listingContent =
                "<html>" + "<head><title>Index of /org/foo/bar</title></head>" + "<body><h1>Index of /org/foo/bar/</h1>"
                        + "<hr><pre>" + "<a href=\"1.0/\">1.0/</a>" + "<a href=\"1.1/\">1.1/</a>"
                        + "</pre><hr></body></html>";
        server.expect( repoRootUrl, 200, listingContent );
        final String datetime = ( new Date() ).toString();
        server.expect( repoSubUrl1, 200, String.format( "metadata %s", datetime ) );
        server.expect( repoSubUrl2, 200, String.format( "metadata %s", datetime ) );

        // set up remote repository pointing to the test http server, and timeout little later
        final String changelog = "Timeout Testing: " + name.getMethodName();
        final RemoteRepository repository =
                new RemoteRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, repoId, server.formatUrl( repoId ) );
        repository.setMetadataTimeoutSeconds( METADATA_TIMEOUT_SECONDS );
        client.stores().create( repository, changelog, RemoteRepository.class );


        try (InputStream is = client.content().get( repository.getKey(), repoSubPath1 ))
        {
        }
        try (InputStream is = client.content().get( repository.getKey(), repoSubPath2 ))
        {
        }

        IndyContentBrowseClientModule browseClientModule = client.module( IndyContentBrowseClientModule.class );

        // first time trigger normal content storage with timeout, should be 4s
        logger.debug("Start to request listing of {}", repoRootPath);
        final ContentBrowseResult content = browseClientModule.getContentList( repository.getKey(), repoRootPath );
        assertThat( "no metadata result", content, notNullValue() );
        logger.debug( "### will begin to get content" );

        final String listingMetaPath = repoRootPath + ".listing.txt";

        File listingMetaFile = Paths.get( fixture.getBootOptions().getHomeDir(), "var/lib/indy/storage", MAVEN_PKG_KEY,
                                          remote.singularEndpointName() + "-" + repoId, listingMetaPath ).toFile();

        assertThat( ".listing doesn't exist: " + listingMetaFile, listingMetaFile.exists(), equalTo( true ) );

        // wait for first time
        Thread.sleep( METADATA_TIMEOUT_WAITING_MILLISECONDS );

        // as the metadata content re-request, the metadata timeout interval should NOT be re-scheduled
        browseClientModule.getContentList( repository.getKey(), repoRootPath );

        // will wait second time for a longer period
        Thread.sleep( METADATA_TIMEOUT_WAITING_MILLISECONDS * getTestTimeoutMultiplier() );

        //        logger.info( "Checking whether metadata file {} has been deleted...", listingMetaFile );
        // as rescheduled, the artifact should not be deleted
        assertThat( "artifact should be removed as the rescheduled of metadata should not succeed",
                    listingMetaFile.exists(), equalTo( false ) );
    }

    @Override
    protected int getTestTimeoutMultiplier()
    {
        return super.getTestTimeoutMultiplier() * 2;
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        writeConfigFile( "main.conf", "remote.list.download.enabled=true\n"
                + "[storage-default]\nstorage.dir=${indy.home}/var/lib/indy/storage" );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.singletonList( new IndyContentBrowseClientModule() );
    }
}
