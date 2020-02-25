/**
 * Copyright (C) 2013 Red Hat, Inc.
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
package org.commonjava.indy.pkg.maven.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * This test is to make sure indy can retrieve huge maven-metadata successfully.
 *
 * <b>GIVEN:</b>
 * <ul>
 *     <li>A remote repo</li>
 *     <li>A test maven-metadata.xml which contains big number of versions</li>
 * </ul>
 *
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Try to get the maven-metadata.xml from the remote repo</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>This metadata file can be retrieved successfully</li>
 * </ul>
 */
public class HugeMetadataDownloadTest
        extends AbstractIndyFunctionalTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    public void run()
            throws Exception
    {

        final String repo = "repo";
        final String path = "org/foo/bar/maven-metadata.xml";

        server.expect( server.formatUrl( repo, path ), 200, getHugeMetadata() );

        RemoteRepository remoteRepo = new RemoteRepository( repo, server.formatUrl( repo ) );
        remoteRepo = client.stores().create( remoteRepo, "adding remote", RemoteRepository.class );

        InputStream stream = client.content().get( remote, remoteRepo.getName(), path );
        assertThat( stream, notNullValue() );

        String metadata = IOUtils.toString( stream );
        stream.close();

        assertThat( metadata, CoreMatchers.notNullValue() );
    }

    private String getHugeMetadata()
    {
        final int latestMajar = 5, latestMinor = 10, latestRelease = 40;
        final String latest = String.format( "%s.%s.%s", latestMajar, latestMinor, latestRelease );

        final StringBuilder builder = new StringBuilder();
        builder.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" )
               .append( "<metadata>" )
               .append( "<groupId>org.foo</groupId>" )
               .append( "<artifactId>bar</artifactId>" )
               .append( "<versioning>" )
               .append( String.format( "<latest>%s</latest>", latest ) )
               .append( String.format( "<release>%s</release>", latest ) )
               .append( "<versions>" );

        for ( int ma = 1; ma <= latestMajar; ma++ )
        {
            for ( int mi = 1; mi <= latestMinor; mi++ )
            {
                for ( int rel = 1; rel <= latestRelease; rel++ )
                {
                    builder.append( String.format( "<version>%s.%s.%s</version>", ma, mi, rel ) );
                }
            }
        }
        builder.append( "</versions>" );
        builder.append( String.format( "<lastUpdated>%s</lastUpdated>", System.currentTimeMillis() ) )
               .append( "</versioning>" )
               .append( "</metadata>" );

        return builder.toString();
    }

}
