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
import org.commonjava.indy.content.index.IndexedStorePath;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.infinispan.BasicCacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.test.http.expect.ExpectationServer;
import org.infinispan.AdvancedCache;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.enterprise.inject.spi.CDI;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>A group contains two remotes</li>
 *     <li>A metadata path which is available in remote 2</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Fetch metadata path through group once</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>The cache entry values should all be key with remote 2 and path with the metadata itself</li>
 * </ul>
 */
public class ContentIndexDirLvWithMetadataTest
        extends AbstractIndyFunctionalTest
{
    private static final String PATH_META = "org/foo/bar/maven-metadata.xml";

    // @formatter:off
    final String PATH_META_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
    // @formatter:on

    @Rule
    public ExpectationServer server = new ExpectationServer();

    private BasicCacheHandle<IndexedStorePath, IndexedStorePath> contentIndex;

    @Before
    public void getIndexManager()
    {
        final ContentIndexCacheProducer cacheProducer = CDI.current().select( ContentIndexCacheProducer.class ).get();
        contentIndex = cacheProducer.contentIndexCacheCfg();
    }

    @Test
    public void test()
            throws Exception
    {

        final String remoteName1 = newName();
        RemoteRepository remote1 = new RemoteRepository( MAVEN_PKG_KEY, remoteName1, server.formatUrl( remoteName1 ) );
        remote1 = client.stores().create( remote1, name.getMethodName(), RemoteRepository.class );

        final String remoteName2 = newName();
        RemoteRepository remote2 = new RemoteRepository( MAVEN_PKG_KEY, remoteName2, server.formatUrl( remoteName2 ) );
        remote2 = client.stores().create( remote2, name.getMethodName(), RemoteRepository.class );

        final String groupName = newName();
        Group group = new Group( MAVEN_PKG_KEY, groupName, remote1.getKey(), remote2.getKey() );
        group = client.stores().create( group, name.getMethodName(), Group.class );

        server.expect( server.formatUrl( remoteName2, PATH_META ), 200, PATH_META_CONTENT );


        try (InputStream s = client.content().get( group.getKey(), PATH_META ))
        {
            assertThat( IOUtils.toString( s ), equalTo( PATH_META_CONTENT ) );
        }


        AdvancedCache<IndexedStorePath, IndexedStorePath> advancedCache =
                (AdvancedCache) contentIndex.execute( c -> c );
        System.out.println( "[Content index DEBUG]: cached isps: " + advancedCache.keySet() );

        for ( IndexedStorePath value : advancedCache.values() )
        {
            assertThat( value.getOriginStoreKey(), equalTo( remote2.getKey() ) );
        }

        System.out.println( "[Content index DEBUG]: cache size:" + advancedCache.size() );
        System.out.println( "[Content index DEBUG]: cache hit:" + advancedCache.getStats().getHits() );
        System.out.println( "[Content index DEBUG]: cache misses:" + advancedCache.getStats().getMisses() );

    }

}
