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
import org.commonjava.indy.content.index.ContentIndexCacheProducer;
import org.commonjava.indy.content.index.ContentIndexManager;
import org.commonjava.indy.content.index.IndexedStorePath;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
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
 *     <li>Path P in repository A that has not yet been downloaded</li>
 *     <li>ContentIndexManager does NOT contain entry for path P in repo A</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Fetch path P from repository A twice</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>After first retrieval of Path P, ContentIndexManager should contain entry for path P in repo A</li>
 *     <li>After second retrieval of Path P, ContentIndexManager should contain THE SAME entry for path P in repo A</li>
 *     <li>IndexedStorePath instances from after first and second retrievals should be the same literal object</li>
 * </ul>
 */
public class ContentIndexRemoteRepoUsageTest
        extends AbstractIndyFunctionalTest
{

    private static final String FIRST_PATH = "org/foo/bar/1/first.txt";

    private static final String FIRST_PATH_CONTENT = "first content";

    @Rule
    public ExpectationServer server = new ExpectationServer();

    private ContentIndexManager indexManager;

    private CacheHandle<IndexedStorePath, StoreKey> cacheHandle;

    @Before
    public void getIndexManager()
    {
        indexManager = CDI.current().select( ContentIndexManager.class ).get();
        cacheHandle = (CacheHandle) CDI.current().select( ContentIndexCacheProducer.class ).get().contentIndexCacheCfg();
    }

    @Test
    public void bypassNotIndexedContentWithAuthoritativeIndex()
            throws Exception
    {
        final String repoName = newName();
        RemoteRepository repo = new RemoteRepository( MAVEN_PKG_KEY, repoName, server.formatUrl( repoName ) );
        repo = client.stores().create( repo, name.getMethodName(), RemoteRepository.class );

        server.expect( server.formatUrl( repoName, FIRST_PATH ), 200, FIRST_PATH_CONTENT );

        StoreKey indexedStoreKey = indexManager.getIndexedStoreKey( repo.getKey(), FIRST_PATH );
        logger.info( "\n\n\nBEFORE: Indexed path entry: " + indexedStoreKey + "\n\n\n\n");
        assertThat( indexedStoreKey, nullValue() );

        Long hits = cacheHandle.executeCache( (cache) -> cache.getAdvancedCache().getStats().getHits() );
        assertThat( hits == 0, equalTo( true ) );

        try (InputStream first = client.content().get( repo.getKey(), FIRST_PATH ))
        {
            assertThat( IOUtils.toString( first ), equalTo( FIRST_PATH_CONTENT ) );
        }

        indexedStoreKey = indexManager.getIndexedStoreKey( repo.getKey(), FIRST_PATH );
        logger.info( "\n\n\nAFTER 1: Indexed path entry: " + indexedStoreKey + "\n\n\n\n");
        assertThat( indexedStoreKey, notNullValue() );

        hits = cacheHandle.executeCache( (cache) -> cache.getAdvancedCache().getStats().getHits() );
        assertThat( hits >= 1, equalTo( true ) );

        try (InputStream first = client.content().get( repo.getKey(), FIRST_PATH ))
        {
            assertThat( IOUtils.toString( first ), equalTo( FIRST_PATH_CONTENT ) );
        }

        StoreKey indexedStoreKey2 = indexManager.getIndexedStoreKey( repo.getKey(), FIRST_PATH );
        logger.info( "\n\n\nAFTER 2: Indexed path entry: " + indexedStoreKey2 + "\n\n\n\n");

        hits = cacheHandle.executeCache( (cache) -> cache.getAdvancedCache().getStats().getHits() );
        assertThat( hits >= 2, equalTo( true ) );

        // object equality should work since we haven't persisted this anywhere yet.
        assertThat( indexedStoreKey == indexedStoreKey2, equalTo( true ) );
    }
}
