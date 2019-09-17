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

import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.io.SpecialPathConstants;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Verifies that when deletes any real artifacts through http, the accompanied
 * http-metadata.json will also be deleted.
 * <br/>
 * When:
 * <ul>
 *     <li>A real artifact but not metadata are stored in a hosted repository</li>
 *     <li>Then the artifact is deleted</li>
 * </ul>
 * <br/>
 * Then:
 * <ul>
 *     <li>The accompanied http-metadata.json for the artifact will be generated after artifact storing</li>
 *     <li>The accompanied http-metadata.json for the artifact will be deleted after artifact deleting</li>
 * </ul>
 */
public class StoreArtifactsAndRemoveThenHttpMetaDeletedTest
        extends AbstractContentManagementTest
{

    @Test
    public void run()
            throws Exception
    {
        final String content = "This is a test: " + System.nanoTime();
        final InputStream stream = new ByteArrayInputStream( content.getBytes() );

        final String dirPath = "/org/foo/bar/1";
        final String path = dirPath + "/bar-1.pom";
        final String httpMetaPath = path + SpecialPathConstants.HTTP_METADATA_EXT;

        StoreKey store = new StoreKey( MAVEN_PKG_KEY, hosted, STORE );
        assertThat( client.content().exists( store, path ), equalTo( false ) );

        client.content().store( store, path, stream );

        assertThat( client.content().exists( store, path ), equalTo( true ) );
        assertThat( client.content().exists( store, httpMetaPath ), equalTo( true ) );

        client.content().delete( store, path );

        assertThat( client.content().exists( store, path ), equalTo( false ) );
        assertThat( client.content().exists( store, httpMetaPath ), equalTo( false ) );
    }
}
