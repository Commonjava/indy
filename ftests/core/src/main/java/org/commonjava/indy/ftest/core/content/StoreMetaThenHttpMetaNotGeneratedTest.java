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
 * Verifies that when stores any metadata through http, the accompanied
 * http-metadata.json will NOT be generated with the http request headers
 * <br/>
 * When:
 * <ul>
 *     <li>Metadatas but not artifacts are stored in a hosted repository</li>
 * </ul>
 * <br/>
 * Then:
 * <ul>
 *     <li>The accompanied http-metadata.json for the metadata will not be generated</li>
 * </ul>
 */
public class StoreMetaThenHttpMetaNotGeneratedTest
        extends AbstractContentManagementTest
{

    @Test
    public void run()
            throws Exception
    {
        final String content = "This is a test: " + System.nanoTime();
        final InputStream stream = new ByteArrayInputStream( content.getBytes() );

        final String dirPath = "/org/foo/bar";
        final String metadataPath = dirPath + "/maven-metadata.xml";
        final String archetypePath = dirPath + "/archetype-catalog.xml";
        final String metadataHttpMetaPath = metadataPath + SpecialPathConstants.HTTP_METADATA_EXT;
        final String archetypeHttpMetaPath = archetypePath + SpecialPathConstants.HTTP_METADATA_EXT;

        StoreKey store = new StoreKey( MAVEN_PKG_KEY, hosted, STORE );

        assertThat( client.content().exists( store, metadataPath ), equalTo( false ) );
        assertThat( client.content().exists( store, archetypePath ), equalTo( false ) );

        client.content().store( store, metadataPath, stream );
        assertThat( client.content().exists( store, metadataPath ), equalTo( true ) );
        assertThat( client.content().exists( store, metadataHttpMetaPath ), equalTo( false ) );

        client.content().store( store, archetypePath, stream );
        assertThat( client.content().exists( store, archetypePath ), equalTo( true ) );
        assertThat( client.content().exists( store, archetypeHttpMetaPath ), equalTo( false ) );

    }
}
