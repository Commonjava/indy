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
package org.commonjava.indy.model.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class RemoteRepositorySerializationTest
{
    @Test
    public void simpleRoundTrip()
            throws IOException, ClassNotFoundException
    {
        RemoteRepository in = new RemoteRepository( PKG_TYPE_MAVEN, "test", "http://nowhere.com/test/repo" );
        RemoteRepository out = doRoundTrip( in );

        compareRepos( in, out );
    }

    @Test
    public void fullRoundTrip()
            throws IOException, ClassNotFoundException
    {
        RemoteRepository in = new RemoteRepository( PKG_TYPE_MAVEN, "test", "http://nowhere.com/test/repo" );
        in.setTimeoutSeconds( -1 );
        in.setPrefetchPriority( 1 );
        in.setMetadataTimeoutSeconds( -1 );
        in.setMetadata( "metadata", "value" );
        in.setPrefetchRescanTimestamp( "2019-08-12T00:00:11.000" );
        in.setCacheTimeoutSeconds( 1000 );
        in.setPassthrough( true );
        in.setPrefetchListingType( "html" );
        in.setServerCertPem( "asdf;alksdjfa;sdlgjgds;lkjs;gkajgd;akjdga;" );
        in.setServerTrustPolicy( "TRUST NO ONE" );
        in.setKeyCertPem( ";lakjdsf;alfdkja;glkjqroipgnevfowenvrwpoerihe" );
        in.setKeyPassword( "key-password" );
        in.setNfcTimeoutSeconds( 10000 );
        in.setPassword( "regular-password" );
        in.setProxyHost( "proxy.host.com" );
        in.setProxyPassword( "proxy-password" );
        in.setProxyPort( 8090 );
        in.setProxyUser( "proxy-user" );
        in.setUser( "regular-user" );
        in.setIgnoreHostnameVerification( true );
        in.setHost( "nowheres.com" );
        in.setMaxConnections( 2000 );
        in.setPort( 1010 );
        in.setDisabled( true );
        in.setRescanInProgress( true );
        in.setDescription( "This is a description" );
        in.setPathMaskPatterns( new HashSet<>( Arrays.asList( "some-path/mask/pattern", null, "another/path" ) ) );
        in.setPathStyle( PathStyle.hashed );
        in.setAllowReleases( false );
        in.setAllowSnapshots( true );
        in.setDisableTimeout( 10 );
        in.setTransientMetadata( "transient", "metadata" );

        RemoteRepository out = doRoundTrip( in );

        compareRepos( in, out );
    }

    private void compareRepos( final RemoteRepository in, final RemoteRepository out )
    {
        assertThat( out.getUrl(), equalTo( in.getUrl() ) );
        assertThat( out.getTimeoutSeconds(), equalTo( in.getTimeoutSeconds() ) );
        assertThat( out.getMaxConnections(), equalTo( in.getMaxConnections() ) );
        assertThat( out.isIgnoreHostnameVerification(), equalTo( in.isIgnoreHostnameVerification() ) );
        assertThat( out.getNfcTimeoutSeconds(), equalTo( in.getNfcTimeoutSeconds() ) );
        assertThat( out.getHost(), equalTo( in.getHost() ) );
        assertThat( out.getPort(), equalTo( in.getPort() ) );
        assertThat( out.getUser(), equalTo( in.getUser() ) );
        assertThat( out.getPassword(), equalTo( in.getPassword() ) );
        assertThat( out.isPassthrough(), equalTo( in.isPassthrough() ) );
        assertThat( out.getCacheTimeoutSeconds(), equalTo( in.getCacheTimeoutSeconds() ) );
        assertThat( out.getMetadataTimeoutSeconds(), equalTo( in.getMetadataTimeoutSeconds() ) );
        assertThat( out.getKeyPassword(), equalTo( in.getKeyPassword() ) );
        assertThat( out.getKeyCertPem(), equalTo( in.getKeyCertPem() ) );
        assertThat( out.getServerCertPem(), equalTo( in.getServerCertPem() ) );
        assertThat( out.getProxyHost(), equalTo( in.getProxyHost() ) );
        assertThat( out.getProxyPort(), equalTo( in.getProxyPort() ) );
        assertThat( out.getProxyUser(), equalTo( in.getProxyUser() ) );
        assertThat( out.getProxyPassword(), equalTo( in.getProxyPassword() ) );
        assertThat( out.getServerTrustPolicy(), equalTo( in.getServerTrustPolicy() ) );
        assertThat( out.getPrefetchPriority(), equalTo( in.getPrefetchPriority() ) );
        assertThat( out.isPrefetchRescan(), equalTo( in.isPrefetchRescan() ) );
        assertThat( out.getPrefetchListingType(), equalTo( in.getPrefetchListingType() ) );
        assertThat( out.getPrefetchRescanTimestamp(), equalTo( in.getPrefetchRescanTimestamp() ) );

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

    private RemoteRepository doRoundTrip( final RemoteRepository in )
            throws IOException, ClassNotFoundException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );

        oos.writeObject( in );

        oos.flush();
        ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );

        ArtifactStore store = (ArtifactStore) ois.readObject();
        return (RemoteRepository) store;
    }
}
