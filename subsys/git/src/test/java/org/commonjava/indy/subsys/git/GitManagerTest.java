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
package org.commonjava.indy.subsys.git;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.commonjava.indy.audit.ChangeSummary.SYSTEM_USER;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.commonjava.indy.audit.ChangeSummary;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GitManagerTest extends AbstractGitManagerTest
{

    @Test
    public void cloneRepoOnStart()
        throws Exception
    {
        final File root = unpackRepo( "test-indy-data.zip" );

        final File cloneDir = temp.newFolder();
        FileUtils.forceDelete( cloneDir );

        final GitConfig config = new GitConfig( cloneDir, root.toURI()
                                                              .toURL()
                                                              .toExternalForm(), true );
        new GitManager( config );
    }

    @Test
    public void addToClonedRepoAndRetrieveCommitLog()
        throws Exception
    {
        final File root = unpackRepo( "test-indy-data.zip" );

        final File cloneDir = temp.newFolder();
        FileUtils.forceDelete( cloneDir );

        final String email = "me@nowhere.com";

        // NOTE: Leave off generation of file-list changed in commit message (third parameter, below)
        final GitConfig config = new GitConfig( cloneDir, root.toURI()
                                                              .toURL()
                                                              .toExternalForm(), false ).setUserEmail( email );
        final GitManager git = new GitManager( config );

        final File f = new File( cloneDir, "test.txt" );
        FileUtils.write( f, "This is a test" );

        final String user = "test";
        final String log = "test commit";
        git.addFiles( new ChangeSummary( user, log ), f );
        git.commit();

        final List<ChangeSummary> changelog = git.getChangelog( f, 0, 1 );

        assertThat( changelog, notNullValue() );
        assertThat( changelog.size(), equalTo( 1 ) );
        assertThat( changelog.get( 0 )
                             .getUser(), equalTo( SYSTEM_USER ) );
        assertThat( changelog.get( 0 )
                             .getSummary().contains( log ), equalTo( true ) );
    }



}
