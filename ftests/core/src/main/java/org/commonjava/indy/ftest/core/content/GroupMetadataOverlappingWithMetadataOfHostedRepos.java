package org.commonjava.indy.ftest.core.content;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.client.core.helper.PathInfo;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class GroupMetadataOverlappingWithMetadataOfHostedRepos
        extends AbstractContentManagementTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    public void run()
            throws Exception
    {
        final String repo1 = "repo1";
        final String repo2 = "repo2";
        final String path = "org/foo/bar/maven-metadata.xml";

        /* @formatter:off */
        final String repo1Content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<metadata>\n" +
            "  <groupId>org.foo</groupId>\n" +
            "  <artifactId>bar</artifactId>\n" +
            "  <versioning>\n" +
            "    <latest>1.0</latest>\n" +
            "    <release>1.0</release>\n" +
            "    <versions>\n" +
            "      <version>1.0</version>\n" +
            "    </versions>\n" +
            "    <lastUpdated>20150722164334</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>\n";
        /* @formatter:on */

        /* @formatter:off */
        final String repo2Content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<metadata>\n" +
            "  <groupId>org.foo</groupId>\n" +
            "  <artifactId>bar</artifactId>\n" +
            "  <versioning>\n" +
            "    <latest>1.1</latest>\n" +
            "    <release>1.1</release>\n" +
            "    <versions>\n" +
            "      <version>1.0</version>\n" +
            "    </versions>\n" +
            "    <lastUpdated>20150722164334</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>\n";
        /* @formatter:on */

        server.expect( server.formatUrl( repo1, path ), 200, repo1Content );
        server.expect( server.formatUrl( repo2, path ), 200, repo2Content );

        RemoteRepository remote1 = new RemoteRepository( repo1, server.formatUrl( repo1 ) );
        remote1 = client.stores().create( remote1, "adding remote", RemoteRepository.class );

        RemoteRepository remote2 = new RemoteRepository( repo2, server.formatUrl( repo2 ) );
        remote2 = client.stores().create( remote2, "adding remote", RemoteRepository.class );

        Group g = new Group( "test", remote1.getKey(), remote2.getKey() );
        g = client.stores().create( g, "adding group", Group.class );

        System.out.printf( "\n\nGroup constituents are:\n  %s\n\n", StringUtils.join( g.getConstituents(), "\n  " ) );

        InputStream stream = client.content().get( group, g.getName(), path );

        assertThat( stream, notNullValue() );

        String metadata = IOUtils.toString( stream );
        assertThat( metadata, equalTo( repo1Content ) );

        final String hostedRepo = "hostedRepo";
        HostedRepository hostedRepository = new HostedRepository( hostedRepo );

        hostedRepository = client.stores().create( hostedRepository, "adding hosted", HostedRepository.class );

        final String metadataFilePath =
                String.format( "%s/var/lib/indy/storage/%s-%s/%s", fixture.getBootOptions().getIndyHome(),
                               hosted.name(), hostedRepo, path );

        final File metadataFile = new File( metadataFilePath );
        if ( !metadataFile.exists() )
        {
            createFileWithDirs(metadataFilePath);
        }

        /* @formatter:off */
        final String hostedMetaContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<metadata>\n" +
            "  <groupId>org.foo</groupId>\n" +
            "  <artifactId>bar</artifactId>\n" +
            "  <versioning>\n" +
            "    <latest>1.2</latest>\n" +
            "    <release>1.2</release>\n" +
            "    <versions>\n" +
            "      <version>1.0</version>\n" +
            "    </versions>\n" +
            "    <lastUpdated>20150722164334</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>\n";
        /* @formatter:on */

        FileWriter writer = new FileWriter( metadataFile );
        writer.write( hostedMetaContent );
        writer.close();

        PathInfo p = client.content().getInfo( hosted, hostedRepo, path );
        assertThat( "hosted metadata should exist", p.exists(), equalTo( true ) );

        g.addConstituent( hostedRepository );

        client.stores().update( g, "add new hosted" );

        System.out.printf( "\n\nUpdated group constituents are:\n  %s\n\n", StringUtils.join( g.getConstituents(), "\n  " ) );

        stream = client.content().get( group, g.getName(), path );

        assertThat( stream, notNullValue() );

        metadata = IOUtils.toString( stream );
        assertThat( metadata, equalTo( hostedMetaContent ) );

    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    private boolean createFileWithDirs( String path )
            throws Exception
    {
        final File file = new File( path );
        if ( file.exists() )
        {
            return false;
        }
        else if ( !file.getParentFile().exists() )
        {
            file.getParentFile().mkdirs();
        }
        return file.createNewFile();
    }
}
