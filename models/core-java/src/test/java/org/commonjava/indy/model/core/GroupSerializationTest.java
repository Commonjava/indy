/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class GroupSerializationTest
{
    @Test
    public void simpleRoundTrip()
            throws IOException, ClassNotFoundException
    {
        Group in = new Group( PKG_TYPE_MAVEN, "test", Arrays.asList( new StoreKey( PKG_TYPE_MAVEN, remote, "test1" ),
                                                                     new StoreKey( PKG_TYPE_MAVEN, remote,
                                                                                   "test2" ) ) );
        Group out = doRoundTrip( in );

        compareRepos( in, out );
    }

    @Test
    public void fullRoundTrip()
            throws IOException, ClassNotFoundException
    {
        Group in = new Group( PKG_TYPE_MAVEN, "test", Arrays.asList( new StoreKey( PKG_TYPE_MAVEN, remote, "test1" ),
                                                                     null,
                                                                     new StoreKey( PKG_TYPE_MAVEN, hosted, "test2" ),
                                                                     new StoreKey( PKG_TYPE_MAVEN, group, "test3" ) ) );

        in.setDescription( "Test description" );
        in.setPrependConstituent( true );
        in.setMetadata( "Test key 1", "foo" );
        in.setDisabled( true );
        in.setRescanInProgress( true );
        in.setPathMaskPatterns( Collections.singleton( "foo" ) );
        in.setPathStyle( PathStyle.hashed );
        in.setAuthoritativeIndex( true );
        in.setDisableTimeout( -1 );
        in.setTransientMetadata( "transient key", "value" );

        Group out = doRoundTrip( in );

        compareRepos( in, out );
    }

    private void compareRepos( final Group in, final Group out )
    {
        assertThat( out.getConstituents(), equalTo( in.getConstituents() ) );
        assertThat( out.isPrependConstituent(), equalTo( in.isPrependConstituent() ) );

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

    private Group doRoundTrip( final Group in )
            throws IOException, ClassNotFoundException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( in );

        oos.flush();
        ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );

        ArtifactStore store = (ArtifactStore) ois.readObject();

        return (Group) store;
    }
}
