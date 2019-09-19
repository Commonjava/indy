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
import org.commonjava.indy.subsys.infinispan.BasicCacheHandle;
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
 *     <li>Several paths with same directory structure but different files</li>
 *     <li>All paths are available in remote 2</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Fetch all paths through group once for each</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>The cache entry values should all be key with remote 2 and path with the directory</li>
 *     <li>The content index cache should contain only 2 cache entries, one for group and one for remote 2</li>
 *     <li>Cache hits should be more than the size of file paths minus 1 </li>
 * </ul>
 */
public class ContentIndexDirLvWithArtifactsTest
        extends AbstractIndyFunctionalTest
{

    private static final String PATH_DIR = "org/foo/bar/1/";
    private static final String PATH_JAR = PATH_DIR + "foobar-1.jar";

    private static final String PATH_JAR_MD5 = PATH_DIR + "foobar-1.jar.md5";

    private static final String PATH_JAR_SHA1 = PATH_DIR + "foobar-1.jar.sha1";

    private static final String PATH_JAR_SHA256 = PATH_DIR + "foobar-1.jar.sha256";

    private static final String PATH_POM = PATH_DIR + "foobar-1.pom";

    private static final String PATH_POM_MD5 = PATH_DIR + "foobar-1.pom.md5";

    private static final String PATH_POM_SHA1 = PATH_DIR + "foobar-1.pom.sha1";

    private static final String PATH_POM_SHA256 = PATH_DIR + "foobar-1.pom.sha256";

    private static final List<String> PATHS_CHECKSUM;

    static
    {
        PATHS_CHECKSUM = Arrays.asList( PATH_JAR_MD5, PATH_JAR_SHA1, PATH_JAR_SHA256, PATH_POM_MD5, PATH_POM_SHA1,
                                        PATH_POM_SHA256 );
    }

    private static final String PATH_JAR_CONTENT = "jar content";

    private static final String PATH_POM_CONTENT = "pom content";

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

        server.expect( server.formatUrl( remoteName2, PATH_JAR ), 200, PATH_JAR_CONTENT );
        server.expect( server.formatUrl( remoteName2, PATH_POM ), 200, PATH_POM_CONTENT );
        final String CHECKSUM_CONTENT = newName();
        for ( String checksumPath : PATHS_CHECKSUM )
        {
            server.expect( server.formatUrl( remoteName2, checksumPath ), 200, CHECKSUM_CONTENT );
        }

        try (InputStream s = client.content().get( group.getKey(), PATH_JAR ))
        {
            assertThat( IOUtils.toString( s ), equalTo( PATH_JAR_CONTENT ) );
        }

        try (InputStream s = client.content().get( group.getKey(), PATH_JAR ))
        {
            assertThat( IOUtils.toString( s ), equalTo( PATH_JAR_CONTENT ) );
        }

        for ( String checksumPath : PATHS_CHECKSUM )
        {
            try (InputStream s = client.content().get( group.getKey(), checksumPath ))
            {
                assertThat( IOUtils.toString( s ), equalTo( CHECKSUM_CONTENT ) );
            }
        }

        AdvancedCache<IndexedStorePath, IndexedStorePath> advancedCache =
                (AdvancedCache) contentIndex.execute( c -> c );
        System.out.println( "[Content index DEBUG]: cached isps: " + advancedCache.keySet() );

        for ( IndexedStorePath value : advancedCache.values() )
        {
            boolean match = remote2.getKey().equals( value.getOriginStoreKey() ) || remote2.getKey()
                                                                                           .equals(
                                                                                                   value.getStoreKey() );
            assertThat( match, equalTo( true ) );
        }

        System.out.println( "[Content index DEBUG]: cache size:" + advancedCache.size() );
        assertTrue( advancedCache.size() <= 2 );
        System.out.println( "[Content index DEBUG]: cache hit:" + advancedCache.getStats().getHits() );
        assertTrue( advancedCache.getStats().getHits() >= 7 );
        System.out.println( "[Content index DEBUG]: cache misses:" + advancedCache.getStats().getMisses() );

    }

}
