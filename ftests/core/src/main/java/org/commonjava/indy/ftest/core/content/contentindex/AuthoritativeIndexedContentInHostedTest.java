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
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * <b>PREREQUISITES:</b>
 * <ul>
 *     <li>org.commonjava.indy.ftest.core.store.HostedAuthIndexWithReadonly must pass</li>
 * </ul>
 *
 * <br />
 * <b>GIVEN:</b>
 * <ul>
 *     <li>Authoritative index of content index enabled</li>
 *     <li>A hosted repo</li>
 *     <li>Store one artifact in repo, so this artifact will be cached in index</li>
 *     <li>Store another artifact in repo through direct file writing, so this artifact will not be cached</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Change the hosted repo to readonly</li>
 *     <li>Change the hosted repo back to non-readonly</li>
 *     <li>Change the hosted repo to readonly again</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>When repo is readonly first time, the cached artifact can be fetched, but the direct writing one can not</li>
 *     <li>When repo is non-readonly, both artifacts are available, and after fetching they are both indexed</li>
 *     <li>When repo is readonly again, both artifacts are still available</li>
 * </ul>
 */
public class AuthoritativeIndexedContentInHostedTest
        extends AbstractIndyFunctionalTest
{

    private static final String CACHED_AFACT_PATH = "org/foo/bar/1/cached.txt";

    private static final String NON_CACHED_AFACT_PATH = "org/foo/bar/2/non_cached.txt";

    private static final String CACHED_CONTENT = "cached content";

    private static final String NON_CACHED_CONTENT = "non cached content";

    @Category( EventDependent.class )
    @Test
    public void bypassNotIndexedContentWithAuthoritativeIndex()
            throws Exception
    {
        final String repoName = newName();
        HostedRepository repo = new HostedRepository( MAVEN_PKG_KEY, repoName );
        repo = client.stores().create( repo, name.getMethodName(), HostedRepository.class );

        client.content()
              .store( repo.getKey(), CACHED_AFACT_PATH, new ByteArrayInputStream( CACHED_CONTENT.getBytes() ) );

        final Path nonCachedFile =
                Paths.get( fixture.getBootOptions().getHomeDir(), "var/lib/indy/storage", MAVEN_PKG_KEY,
                           hosted.singularEndpointName() + "-" + repoName, NON_CACHED_AFACT_PATH );
        Files.createDirectories( nonCachedFile.getParent() );
        Files.createFile( nonCachedFile );

        try (FileOutputStream stream = new FileOutputStream( nonCachedFile.toFile() ))
        {
            stream.write( NON_CACHED_CONTENT.getBytes() );
        }

        repo.setReadonly( true );
        assertThat( client.stores().update( repo, name.getMethodName() ), equalTo( true ) );

        Thread.sleep( 500 );

        try (InputStream cached = client.content().get( repo.getKey(), CACHED_AFACT_PATH ))
        {
            assertThat( IOUtils.toString( cached ), equalTo( CACHED_CONTENT ) );
        }

        try (InputStream cached = client.content().get( repo.getKey(), NON_CACHED_AFACT_PATH ))
        {
            assertThat( cached, equalTo( null ) );
        }

        repo.setReadonly( false );
        assertThat( client.stores().update( repo, name.getMethodName() ), equalTo( true ) );

        Thread.sleep( 500 );

        try (InputStream cached = client.content().get( repo.getKey(), CACHED_AFACT_PATH ))
        {
            assertThat( IOUtils.toString( cached ), equalTo( CACHED_CONTENT ) );
        }

        try (InputStream nonCached = client.content().get( repo.getKey(), NON_CACHED_AFACT_PATH ))
        {
            assertThat( IOUtils.toString( nonCached ), equalTo( NON_CACHED_CONTENT ) );
        }

        repo.setReadonly( true );
        assertThat( client.stores().update( repo, name.getMethodName() ), equalTo( true ) );

        Thread.sleep( 500 );

        try (InputStream cached = client.content().get( repo.getKey(), CACHED_AFACT_PATH ))
        {
            assertThat( IOUtils.toString( cached ), equalTo( CACHED_CONTENT ) );
        }

        try (InputStream nonCached = client.content().get( repo.getKey(), NON_CACHED_AFACT_PATH ))
        {
            assertThat( IOUtils.toString( nonCached ), equalTo( NON_CACHED_CONTENT ) );
        }

    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestConfig( fixture );
        writeConfigFile( "conf.d/content-index.conf", "[content-index]\nsupport.authoritative.indexes=true" );
    }
}
