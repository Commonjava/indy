/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.pkg.npm.content;

import org.commonjava.indy.client.core.helper.PathInfo;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.ftest.core.category.TimingDependent;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.pkg.PackageTypeConstants;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This case tests if remote package.json will timeout correctly
 * when: <br />
 * <ul>
 *      <li>remote repo A contains package.json with real content for a given package</li>
 *      <li>package.json is retrieved via remote repo A by client</li>
 *      <li>package.json metadata timeout is scheduled</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>package.json expires with metadata timeout and is removed from local storage in remote repository A</li>
 * </ul>
 */
public class NPMMetadataWithContentTimeoutTest
        extends AbstractContentManagementTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    @Test
    @Category( TimingDependent.class )
    public void timeout()
            throws Exception
    {
        final int METADATA_TIMEOUT_SECONDS = 4;
        final int METADATA_TIMEOUT_WAITING_MILLISECONDS = 2500;

        final String repoId = "test-repo";
        final String pkgPath = "@babel/parser";
        final String pkgMetaPath = pkgPath + "/package.json";

        final String metadataUrl = server.formatUrl( repoId, pkgPath );

        // mocking up a http server that expects access to metadata
        try (InputStream is = getClass().getResourceAsStream( "babel-parser-7.0.0-beta.48.json" ))
        {
            server.expect( metadataUrl, 200, is );
        }

        // set up remote repository pointing to the test http server, and timeout little later
        final String changelog = "Timeout Testing: " + name.getMethodName();
        final RemoteRepository repository =
                new RemoteRepository( PackageTypeConstants.PKG_TYPE_NPM, repoId, server.formatUrl( repoId ) );
        repository.setMetadataTimeoutSeconds( METADATA_TIMEOUT_SECONDS );
        client.stores().create( repository, changelog, RemoteRepository.class );

        // first time trigger normal content storage with timeout, should be 4s
        PathInfo pkgPathFile = client.content().getInfo( repository.getKey(), pkgPath );
        client.content().get( repository.getKey(), pkgPath ).close(); // force storage
        assertThat( "no metadata result", pkgPathFile, notNullValue() );
        assertThat( "metadata doesn't exist", pkgPathFile.exists(), equalTo( true ) );

        pkgPathFile = client.content().getInfo( repository.getKey(), pkgMetaPath );
        client.content().get( repository.getKey(), pkgPath ).close();
        assertThat( "no metadata result", pkgPathFile, notNullValue() );
        assertThat( "metadata doesn't exist", pkgPathFile.exists(), equalTo( true ) );

        Location location = LocationUtils.toLocation( repository );
        File pkgFile = getPhysicalStorageFile( location, pkgPath );
        File pkgMetaFile = getPhysicalStorageFile( location, pkgMetaPath );

        assertThat( "pkg dir doesn't exist", pkgFile.exists(), equalTo( true ) );
        assertThat( "metadata doesn't exist", pkgMetaFile.exists(), equalTo( true ) );

        // wait for first 2.5s
        sleepAndRunFileGC( METADATA_TIMEOUT_WAITING_MILLISECONDS );

        // as the metadata content re-request, the metadata timeout interval should NOT be re-scheduled
        client.content().get( repository.getKey(), pkgPath ).close();
        client.content().get( repository.getKey(), pkgMetaPath ).close();

        // will wait another 4s
        sleepAndRunFileGC( METADATA_TIMEOUT_WAITING_MILLISECONDS + 1500 );
        // as rescheduled, the artifact should not be deleted
        assertThat( "artifact should be removed as the rescheduled of metadata should not succeed",
                    pkgFile.exists(), equalTo( false ) );
        assertThat( "artifact should be removed as the rescheduled of metadata should not succeed",
                    pkgMetaFile.exists(), equalTo( false ) );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
