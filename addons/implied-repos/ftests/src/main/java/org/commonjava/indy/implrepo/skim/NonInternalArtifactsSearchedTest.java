/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.implrepo.skim;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.client.core.helper.PathInfo;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.parse.PomPeek;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * This case test if the implied remote repo can successfully exclude all redhat internal artifacts. It does this:
 * when: <br />
 * <ul>
 *      <li>create group with a remote repo</li>
 *      <li>remote repo contains repositories</li>
 *      <li>enable implied repo for group</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the implied remote will be created for the repositories in remote</li>
 *     <li>normal artifacts can be fetched correctly</li>
 *     <li>redhat internal artifacts can not be fetched</li>
 * </ul>
 */
public class NonInternalArtifactsSearchedTest
        extends AbstractSkimFunctionalTest
{
    private static final String REPO = "i-repo-one";

    @Test
    public void checkArtifactsFetching()
            throws Exception
    {
        final PomRef ref = loadPom( "one-repo", Collections.singletonMap( "one-repo.url", server.formatUrl( REPO ) ) );
        server.expect( server.formatUrl( TEST_REPO, ref.path ), 200, ref.pom );

        final InputStream stream = client.content().get( StoreType.group, PUBLIC, ref.path );
        final String downloaded = IOUtils.toString( stream );
        IOUtils.closeQuietly( stream );

        assertThat( "SANITY: downloaded POM is wrong!", downloaded, equalTo( ref.pom ) );

        // sleep while event observer runs...
        System.out.println( "Waiting 5s for events to run." );
        Thread.sleep( 5000 );

        final PomPeek peek = new PomPeek( ref.pom, false );
        final ProjectVersionRef gav = peek.getKey();
        final String internalPath =
                String.format( "%s/%s/%s/%s-%s-redhat-1.pom", gav.getGroupId().replace( '.', '/' ), gav.getArtifactId(),
                               gav.getVersionString(), gav.getArtifactId(), gav.getVersionString() );
        server.expect( server.formatUrl( REPO, ref.path ), 200, ref.pom );
        server.expect( server.formatUrl( REPO, internalPath ), 200, ref.pom );

        RemoteRepository r = client.stores().load( StoreType.remote, REPO, RemoteRepository.class );
        assertNotNull( r );
        assertThat( r.getName(), equalTo( REPO ) );

        PathInfo path = client.content().getInfo( StoreType.remote, REPO, ref.path );
        assertNotNull( path );
        assertThat( path.exists(), equalTo( true ) );

        path = client.content().getInfo( StoreType.remote, REPO, internalPath );
        assertNotNull( path );
        assertThat( path.exists(), equalTo( false ) );

    }
}
