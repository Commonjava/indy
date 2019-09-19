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
import org.commonjava.indy.model.core.ArtifactStore;
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
 *     <li>Group TG and G, TG contains G, G contains repository A</li>
 *     <li>Path P in repository A that has not been downloaded</li>
 *     <li>ContentIndexManager does NOT contain entry for path P in TG, G, and A</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Fetch path P from TG</li>
 *     <li>Delete TG</li>
 *     <li>Delete A</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>After retrieval of Path P, ContentIndexManager should contain entries for path P in TG, G, and A</li>
 *     <li>After delete TG, ContentIndexManager should not contain entry for path P in TG, still contain entries in G and A</li>
 *     <li>After delete A, ContentIndexManager should not contain entries for path P in G and A</li>
 * </ul>
 */
public class ContentIndexNestedGroupAndStoreDeletionTest
        extends AbstractIndyFunctionalTest
{

    private static final String PATH = "org/foo/bar/1/test.txt";

    private static final String CONTENT = "This is a test";

    @Rule
    public ExpectationServer server = new ExpectationServer();

    private ContentIndexManager indexManager;

    private RemoteRepository repositoryA;

    private Group group, topGroup; // topGroup contains group, group contains repositoryA

    @Before
    public void setUp() throws Exception
    {
        final String repoA = "A";

        final String gNameG = "G";

        final String gNameTG = "TG";

        indexManager = CDI.current().select( ContentIndexManager.class ).get();

        repositoryA = new RemoteRepository( MAVEN_PKG_KEY, repoA, server.formatUrl( repoA ) );
        repositoryA = client.stores().create( repositoryA, name.getMethodName(), RemoteRepository.class );

        server.expect( server.formatUrl( repoA, PATH ), 200, CONTENT );

        group = new Group( MAVEN_PKG_KEY, gNameG, repositoryA.getKey() );
        group = client.stores().create( group, name.getMethodName(), Group.class );

        topGroup = new Group( MAVEN_PKG_KEY, gNameTG, group.getKey() );
        topGroup = client.stores().create( topGroup, name.getMethodName(), Group.class );
    }

    @Test
    public void bypassNotIndexedContentWithAuthoritativeIndex()
            throws Exception
    {
        // Fetch path P from TG
        try (InputStream first = client.content().get( topGroup.getKey(), PATH ))
        {
            assertThat( IOUtils.toString( first ), equalTo( CONTENT ) );
        }

        indexExist( topGroup, group );

        client.stores().delete( topGroup.getKey(), name.getMethodName() ); // delete TG

        Thread.sleep( 2000 );

        indexNotExist( topGroup );

        indexExist( group );

        client.stores().delete( repositoryA.getKey(), name.getMethodName() ); // delete A

        Thread.sleep( 2000 );

        indexNotExist( group );

    }

    private void indexNotExist( ArtifactStore... stores )
    {
        for ( ArtifactStore store : stores )
        {
            StoreKey indexedStoreKey = indexManager.getIndexedStoreKey( store.getKey(), PATH );
            assertThat( "Indexed StoreKey was found for " + store.getKey(), indexedStoreKey, nullValue() );
        }
    }

    private void indexExist( ArtifactStore... stores )
    {
        for ( ArtifactStore store : stores )
        {
            StoreKey indexedStoreKey = indexManager.getIndexedStoreKey( store.getKey(), PATH );
            logger.debug( "\n\n\nGot indexedStoreKey: " + indexedStoreKey + "\n\n\n" );
            assertThat( "Indexed StoreKey not found for " + store.getKey(), indexedStoreKey, notNullValue() );
        }
    }
}
