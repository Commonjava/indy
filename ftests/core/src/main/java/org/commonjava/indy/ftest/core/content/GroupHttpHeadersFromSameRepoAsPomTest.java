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
package org.commonjava.indy.ftest.core.content;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientHttp;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.module.IndyRawHttpModule;
import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.util.HttpUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Created by jdcasey on 2/6/17.
 *
 * <b>GIVEN:</b>
 * <ul>
 *     <li>{@link Group} A contains ordered membership of {@link RemoteRepository}s [X,Y]</li>
 *     <li>Path P in {@link RemoteRepository} X <b>has not</b> been downloaded</li>
 *     <li>HEAD request for Pom P in {@link RemoteRepository} X <b>has</b> been executed, resulting in creation of the associated http-metadata.json</li>
 *     <li>Path P in {@link RemoteRepository} Y <b>has</b> been downloaded previously, also resulting in creation of associated http-metadata.json</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Pom P is requested from {@link Group} A</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>{@link Group} A returns http-metadata.json associated with the Pom in {@link RemoteRepository} Y</li>
 * </ul>
 */
public class GroupHttpHeadersFromSameRepoAsPomTest
        extends AbstractContentManagementTest
{

    private static final String REPO_X = "X";

    private static final String REPO_Y = "Y";

    private static final String GROUP_A = "A";

    private static final String CONTENT_1 = "This is content #1.";

    private static final String CONTENT_2 = "This is content #2. Some more content, here.";

    private static final String PATH = "/path/to/test.txt";

    private RemoteRepository repoX;

    private RemoteRepository repoY;

    private Group groupA;

    private byte[] content2;

    private final IndyRawHttpModule httpModule = new IndyRawHttpModule();

    @Before
    public void setupStores()
            throws Exception
    {
        String changelog = "test setup";
        repoX = client.stores().create( new RemoteRepository( REPO_X, server.formatUrl( REPO_X ) ), changelog, RemoteRepository.class );
        repoY = client.stores().create( new RemoteRepository( REPO_Y, server.formatUrl( REPO_Y ) ), changelog, RemoteRepository.class );

        content2 = CONTENT_2.getBytes( "UTF-8" );

        server.expect( server.formatUrl( REPO_X, PATH ), 200, CONTENT_1 );

        server.expect( server.formatUrl( REPO_Y, PATH ), 200, new ByteArrayInputStream( content2 ) );

        groupA = client.stores().create( new Group( GROUP_A, repoX.getKey(), repoY.getKey() ), changelog, Group.class );

    }

    @Test
    public void run()
            throws IndyClientException, IOException
    {
        assertContent( repoX, PATH, CONTENT_1 );
        assertContent( repoY, PATH, CONTENT_2 );

//        repoX.setDisabled( true );
//        client.stores().update( repoX, "disabling" );

        File remoteXFile = Paths.get( fixture.getBootOptions().getHomeDir(), "var/lib/indy/storage", MAVEN_PKG_KEY,
                                      remote.singularEndpointName() + "-X", PATH ).toFile();

        waitForEventPropagation();

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Deleting main file: {} (leaving associated http-metadata.json file in place)", remoteXFile );

        assertThat( "Failed to delete: " + remoteXFile, remoteXFile.delete(), equalTo( true ) );

        waitForEventPropagation();

        assertContentLength( groupA, PATH, content2.length );
        assertContentLength( repoX, PATH, CONTENT_1.getBytes().length );
        assertContentLength( repoY, PATH, content2.length );
    }

    private void assertContentLength( ArtifactStore store, String path, int length )
            throws IndyClientException
    {
        IndyClientHttp http = httpModule.getHttp();
        Map<String, String> headers = http.head( client.content().contentPath( store.getKey(), path ) );

        assertThat( headers, notNullValue() );

        String clStr = headers.get( "content-length" );
        assertThat( clStr, notNullValue() );

        Long len = Long.parseLong( clStr );
        assertThat( "Error: " + store.getKey() + ":" + path + ": had incorrect retrieved content-length (" + len
                            + "); should have been: " + length, len, equalTo( Long.valueOf( length ) ) );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Arrays.asList( httpModule );
    }
}
