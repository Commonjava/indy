package org.commonjava.indy.ftest.core.content;

import org.commonjava.indy.client.core.helper.PathInfo;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;

import java.io.File;
import java.util.Date;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public abstract class AbstractContentTimeoutWorkingTest
        extends AbstractContentManagementTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    private String pomFilePath;

    protected static final int TIMEOUT_SECONDS = 2;

    protected static final int TIMEOUT_WAITING_MILLISECONDS = ( TIMEOUT_SECONDS + 2 ) * 1000;

    @Before
    public void setupRepo()
            throws Exception
    {
        final String repoId = "test-repo";
        final String pomPath = "org/foo/bar/1.0/bar-1.0.pom";
        final String pomUrl = server.formatUrl( repoId, pomPath );

        // mocking up a http server that expects access to a .pom
        final String datetime = ( new Date() ).toString();
        server.expect( pomUrl, 200, String.format( "pom %s", datetime ) );

        // set up remote repository pointing to the test http server, and timeout little later
        final String changelog = "Timeout Testing: " + name.getMethodName();
        final RemoteRepository repository = createRemoteRepository( repoId );

        client.stores().create( repository, changelog, RemoteRepository.class );

        // ensure the pom exist before the timeout checking
        final PathInfo result = client.content().getInfo( remote, repoId, pomPath );
        assertThat( "no result", result, notNullValue() );
        assertThat( "doesn't exist", result.exists(), equalTo( true ) );
        pomFilePath = String.format( "%s/var/lib/indy/storage/%s-%s/%s", fixture.getBootOptions().getIndyHome(),
                                     remote.name(), repoId, pomPath );
        final File pomFile = new File( pomFilePath );
        assertThat( "pom doesn't exist", pomFile.exists(), equalTo( true ) );

    }

    protected void fileCheckingAfterTimeout()
            throws Exception
    {
        // make sure the repo timout
        Thread.sleep( TIMEOUT_WAITING_MILLISECONDS );
        logger.debug( "Timeout time {}s passed!", TIMEOUT_SECONDS );

        final File pomFile = new File( pomFilePath );
        assertThat( "artifact should be removed when timeout", pomFile.exists(), equalTo( false ) );
    }

    protected abstract RemoteRepository createRemoteRepository( String repoId );
}
