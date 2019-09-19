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
package org.commonjava.indy.ftest.core.content.contentindex;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.content.index.ContentIndexManager;
import org.commonjava.indy.content.index.IndexedStorePath;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.enterprise.inject.spi.CDI;
import java.io.InputStream;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>Remote repository A</li>
 *     <li>Group G, containing repository A</li>
 *     <li>Path P in repository A that has not yet been downloaded</li>
 *     <li>ContentIndexManager does NOT contain entry for path P in repo A</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Fetch path P from repository A</li>
 *     <li>Fetch path P from group B</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>After first retrieval of Path P, ContentIndexManager should contain entry for path P in repo A</li>
 *     <li>After second retrieval of Path P, ContentIndexManager should containentry for path P in group B</li>
 * </ul>
 */
public class ContentIndexGroupUsageTest
        extends AbstractIndyFunctionalTest
{

    private static final String FIRST_PATH = "org/foo/bar/1/first.txt";

    private static final String FIRST_PATH_CONTENT = "first content";

    @Rule
    public ExpectationServer server = new ExpectationServer();

    private ContentIndexManager indexManager;

    @Before
    public void getIndexManager()
    {
        indexManager = CDI.current().select( ContentIndexManager.class ).get();
    }

    @Test
    public void bypassNotIndexedContentWithAuthoritativeIndex()
            throws Exception
    {
        final String repoName = newName();
        RemoteRepository repo = new RemoteRepository( MAVEN_PKG_KEY, repoName, server.formatUrl( repoName ) );
        repo = client.stores().create( repo, name.getMethodName(), RemoteRepository.class );

        server.expect( server.formatUrl( repoName, FIRST_PATH ), 200, FIRST_PATH_CONTENT );

        String groupName = newName();
        Group group = new Group( MAVEN_PKG_KEY, groupName, repo.getKey() );
        group = client.stores().create( group, name.getMethodName(), Group.class );

        StoreKey indexedStoreKey = indexManager.getIndexedStoreKey( repo.getKey(), FIRST_PATH );
        logger.info( "\n\n\nBEFORE: Indexed path entry: " + indexedStoreKey + "\n\n\n\n");
        assertThat( indexedStoreKey, nullValue() );

        try (InputStream first = client.content().get( repo.getKey(), FIRST_PATH ))
        {
            assertThat( IOUtils.toString( first ), equalTo( FIRST_PATH_CONTENT ) );
        }

        indexedStoreKey = indexManager.getIndexedStoreKey( repo.getKey(), FIRST_PATH );
        logger.info( "\n\n\nAFTER remote: Remote-level indexed path entry: " + indexedStoreKey + "\n\n\n\n");
        assertThat( indexedStoreKey, notNullValue() );

        try (InputStream first = client.content().get( group.getKey(), FIRST_PATH ))
        {
            assertThat( IOUtils.toString( first ), equalTo( FIRST_PATH_CONTENT ) );
        }

        indexedStoreKey = indexManager.getIndexedStoreKey( group.getKey(), FIRST_PATH );
        logger.info( "\n\n\nAFTER group: Group-level indexed path entry: " + indexedStoreKey + "\n\n\n\n");
        assertThat( indexedStoreKey, notNullValue() );

//        indexedStoreKey = indexManager.getIndexedStoreKey( repo.getKey(), FIRST_PATH );
//        logger.info( "\n\n\nAFTER group: Remote-level indexed path entry: " + indexedStoreKey + "\n\n\n\n");
//        assertThat( indexedStoreKey, notNullValue() );
    }
}
