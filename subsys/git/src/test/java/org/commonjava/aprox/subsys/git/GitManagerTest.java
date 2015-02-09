package org.commonjava.aprox.subsys.git;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
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
import org.commonjava.aprox.audit.ChangeSummary;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GitManagerTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void cloneRepoOnStart()
        throws Exception
    {
        final File root = unpackRepo( "test-aprox-data.zip" );

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
        final File root = unpackRepo( "test-aprox-data.zip" );

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
        git.addAndCommitFiles( new ChangeSummary( user, log ), f );

        final List<ChangeSummary> changelog = git.getChangelog( f, 0, 1 );

        assertThat( changelog, notNullValue() );
        assertThat( changelog.size(), equalTo( 1 ) );
        assertThat( changelog.get( 0 )
                             .getUser(), equalTo( user ) );
        assertThat( changelog.get( 0 )
                             .getSummary(), equalTo( log ) );
    }

    private File unpackRepo( final String resource )
        throws Exception
    {
        final URL url = Thread.currentThread()
                              .getContextClassLoader()
                              .getResource( resource );

        final InputStream stream = url.openStream();
        final ZipInputStream zstream = new ZipInputStream( stream );

        final File dir = temp.newFolder();

        ZipEntry entry = null;
        while ( ( entry = zstream.getNextEntry() ) != null )
        {
            final File f = new File( dir, entry.getName() );
            if ( entry.isDirectory() )
            {
                f.mkdirs();
            }
            else
            {
                f.getParentFile()
                 .mkdirs();
                final OutputStream out = new FileOutputStream( f );

                copy( zstream, out );

                closeQuietly( out );
            }

            zstream.closeEntry();
        }

        closeQuietly( zstream );

        final File root = new File( dir, "test-aprox-data/.git" );
        assertThat( root.exists(), equalTo( true ) );
        assertThat( root.isDirectory(), equalTo( true ) );

        return root;
    }

}
