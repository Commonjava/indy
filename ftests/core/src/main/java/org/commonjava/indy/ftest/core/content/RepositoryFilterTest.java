package org.commonjava.indy.ftest.core.content;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.commonjava.indy.core.content.group.GroupRepositoryFilterManager.REPO_FILTER;
import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.commonjava.indy.subsys.template.ScriptEngine.SCRIPTS_SUBDIR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Given:
 *   - Remote repo R, contains path P1
 *   - Hosted repo build-1 contains path P2 (match -rh* pattern)
 *   - Hosted repo build-1 contains path P1 (to confuse retrieval process)
 *   - Group G contains build-1, R
 *
 * When:
 *   - Get path P1 from group G
 *
 * Then:
 *   - Ignore the hosted build-1 and get the content from remote R
 *
 * When:
 *   - Get path P2 from group G
 *
 * Then:
 *   - Get content from hosted build-1
 */
public class RepositoryFilterTest
                extends AbstractContentManagementTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    public void run() throws Exception
    {
        final String path_1 = "org/foo/bar/1.0/bar-1.0.pom";
        final String content_1 = "This is a test";

        final String path_2 = "org/foo/bar/1.0/bar-1.0-rh-0001.pom";
        final String content_2 = "This is a rh test";

        final String remoteR = "R";
        final String hostedBuild_1 = "build-1";
        final String groupG = "G";

        server.expect( server.formatUrl( remoteR, path_1 ), 200, new ByteArrayInputStream( content_1.getBytes() ) );
        RemoteRepository remote = client.stores()
                                        .create( new RemoteRepository( MAVEN_PKG_KEY, remoteR,
                                                                       server.formatUrl( remoteR ) ), "Add remote",
                                                 RemoteRepository.class );

        HostedRepository hosted = client.stores()
                                        .create( new HostedRepository( MAVEN_PKG_KEY, hostedBuild_1 ), "Add hosted",
                                                 HostedRepository.class );
        client.content().store( hosted.getKey(), path_1, new ByteArrayInputStream( content_1.getBytes() ) );
        client.content().store( hosted.getKey(), path_2, new ByteArrayInputStream( content_2.getBytes() ) );

        Group g = new Group( MAVEN_PKG_KEY, groupG, hosted.getKey(), remote.getKey() );
        g = client.stores().create( g, "Add group", Group.class );

        System.out.printf( "\n\nGroup constituents are:\n  %s\n\n", StringUtils.join( g.getConstituents(), "\n  " ) );

        //
        try (InputStream stream = client.content().get( g.getKey(), path_1 ))
        {
            String str = IOUtils.toString( stream );
            assertThat( str, equalTo( content_1 ) );
        }

        try (InputStream stream = client.content().get( g.getKey(), path_2 ))
        {
            String str = IOUtils.toString( stream );
            assertThat( str, equalTo( content_2 ) );
        }
    }

    @Override
    protected void initTestData( CoreServerFixture fixture ) throws IOException
    {
        String filterFile = "rh-pattern-repofilter.groovy";
        copyToDataFile( "repofilter/" + filterFile, SCRIPTS_SUBDIR + "/" + REPO_FILTER + "/" + filterFile );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
