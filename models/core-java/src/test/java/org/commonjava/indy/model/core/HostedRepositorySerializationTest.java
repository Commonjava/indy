/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.model.core;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;

import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class HostedRepositorySerializationTest
{
    @Test
    public void simpleRoundTrip()
            throws IOException, ClassNotFoundException
    {
        HostedRepository in = new HostedRepository( PKG_TYPE_MAVEN, "test" );
        HostedRepository out = doRoundTrip( in );

        compareRepos( in, out );
    }

    @Test
    public void fullRoundTrip()
            throws IOException, ClassNotFoundException
    {
        HostedRepository in = new HostedRepository( PKG_TYPE_MAVEN, "test" );

        in.setReadonly( true );
        in.setAuthoritativeIndex( true );
        in.setSnapshotTimeoutSeconds( 100 );
        in.setStorage( "/path/to/alt/storage" );
        in.setMetadata( "foo", "bar" );
        in.setDisabled( true );
        in.setRescanInProgress( true );
        in.setDescription( "This is a description" );
        in.setPathMaskPatterns( Collections.singleton( "somepath" ) );
        in.setPathStyle( PathStyle.hashed );
        in.setAllowReleases( false );
        in.setAllowSnapshots( true );
        in.setDisableTimeout( -1 );
        in.setTransientMetadata( "transient", "value" );

        HostedRepository out = doRoundTrip( in );

        compareRepos( in, out );
    }

    private void compareRepos( final HostedRepository in, final HostedRepository out )
    {
        assertThat( out.getStorage(), equalTo( in.getStorage() ) );
        assertThat( out.getSnapshotTimeoutSeconds(), equalTo( in.getSnapshotTimeoutSeconds() ) );
        assertThat( out.isReadonly(), equalTo( in.isReadonly() ) );

        assertThat( out.isAllowReleases(), equalTo( in.isAllowReleases() ) );
        assertThat( out.isAllowSnapshots(), equalTo( in.isAllowSnapshots() ) );

        assertThat( out.getKey(), equalTo( in.getKey() ) );
        assertThat( out.getDescription(), equalTo( in.getDescription() ) );
        assertThat( out.getMetadata(), equalTo( in.getMetadata() ) );
        assertThat( out.isDisabled(), equalTo( in.isDisabled() ) );
        assertThat( out.getDisableTimeout(), equalTo( in.getDisableTimeout() ) );
        assertThat( out.getPathStyle(), equalTo( in.getPathStyle() ) );
        assertThat( out.getPathMaskPatterns(), equalTo( in.getPathMaskPatterns() ) );
        assertThat( out.isAuthoritativeIndex(), equalTo( in.isAuthoritativeIndex() ) );
        assertThat( out.getCreateTime(), equalTo( in.getCreateTime() ) );
        assertThat( out.isRescanInProgress(), equalTo( in.isRescanInProgress() ) );

        if ( out.getTransientMetadata() != null && !out.getTransientMetadata().isEmpty())
        {
            fail( "Transient metadata should be empty from deserialized object. Was: " + out.getTransientMetadata() );
        }
    }

    private HostedRepository doRoundTrip( final HostedRepository in )
            throws IOException, ClassNotFoundException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( in );

        oos.flush();
        ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );

        ArtifactStore store = (ArtifactStore) ois.readObject();
        return (HostedRepository) store;
    }
}
