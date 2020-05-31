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
package org.commonjava.indy.repo.proxy;

import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.util.Optional;

import static org.commonjava.indy.repo.proxy.RepoProxyUtils.extractPath;
import static org.commonjava.indy.repo.proxy.RepoProxyUtils.getOriginalStoreKeyFromPath;
import static org.commonjava.indy.repo.proxy.RepoProxyUtils.getProxyTo;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class RepoProxyUtilsTest
{
    @Test
    public void testGetOriginalStoreKeyFromPath()
    {
        String path = "/api/content/maven/group/abc/org/commonjava/indy/maven-metadata.xml";
        Optional<String> storeKeyStr = getOriginalStoreKeyFromPath( path );
        assertTrue( storeKeyStr.isPresent() );
        assertThat( storeKeyStr.get(), equalTo( "maven:group:abc" ) );

        path = "/api/content/maven/group/builds-untested+shared-imports/org/commonjava/indy/maven-metadata.xml";
        storeKeyStr = getOriginalStoreKeyFromPath( path );
        assertTrue( storeKeyStr.isPresent() );
        assertThat( storeKeyStr.get(), equalTo( "maven:group:builds-untested+shared-imports" ) );

        path = "/api/content/maven/hosted/def";
        storeKeyStr = getOriginalStoreKeyFromPath( path );
        assertTrue( storeKeyStr.isPresent() );
        assertThat( storeKeyStr.get(), equalTo( "maven:hosted:def" ) );

        path = "/api/content/npm/group/ghi/";
        storeKeyStr = getOriginalStoreKeyFromPath( path );
        assertTrue( storeKeyStr.isPresent() );
        assertThat( storeKeyStr.get(), equalTo( "npm:group:ghi" ) );

        path = "/api/content/npm/groupe/ghi/";
        storeKeyStr = getOriginalStoreKeyFromPath( path );
        assertFalse( storeKeyStr.isPresent() );
    }

    @Test
    public void testGetOriginalStoreKeyFromLegacyPath(){
        String path = "/api/group/abc/org/commonjava/indy/maven-metadata.xml";
        Optional<String> storeKeyStr = getOriginalStoreKeyFromPath( path );
        assertTrue( storeKeyStr.isPresent() );
        assertThat( storeKeyStr.get(), equalTo( "maven:group:abc" ) );

        path = "/api/group/builds-untested+shared-imports/org/commonjava/indy/maven-metadata.xml";
        storeKeyStr = getOriginalStoreKeyFromPath( path );
        assertTrue( storeKeyStr.isPresent() );
        assertThat( storeKeyStr.get(), equalTo( "maven:group:builds-untested+shared-imports" ) );

        path = "/api/hosted/def";
        storeKeyStr = getOriginalStoreKeyFromPath( path );
        assertTrue( storeKeyStr.isPresent() );
        assertThat( storeKeyStr.get(), equalTo( "maven:hosted:def" ) );
    }

    @Test
    public void testExtractPath()
    {
        String fullPath = "/api/content/maven/group/abc/org/commonjava/indy/maven-metadata.xml";
        String path = extractPath( fullPath );
        assertThat( path, equalTo( "/org/commonjava/indy/maven-metadata.xml" ) );

        fullPath = "/api/content/maven/group/builds-untested+shared-imports/org/commonjava/indy/maven-metadata.xml";
        path = extractPath( fullPath );
        assertThat( path, equalTo( "/org/commonjava/indy/maven-metadata.xml" ) );

        fullPath = "/api/content/maven/hosted/def/org/commonjava/indy/1.0/";
        path = extractPath( fullPath );
        assertThat( path, equalTo( "/org/commonjava/indy/1.0/" ) );

        fullPath = "/api/content/npm/group/ghi/org/commonjava/indy/indy-api/1.0/indy-api-1.0.pom";
        path = extractPath( fullPath );
        assertThat( path, equalTo( "/org/commonjava/indy/indy-api/1.0/indy-api-1.0.pom" ) );

        fullPath = "/api/content/npm/hosted/jkl/org/commonjava/indy/indy-api/2.0/indy-api-2.0.jar";
        path = extractPath( fullPath );
        assertThat( path, equalTo( "/org/commonjava/indy/indy-api/2.0/indy-api-2.0.jar" ) );

        fullPath = "/api/content/npm/hosted/jkl/";
        path = extractPath( fullPath );
        assertThat( path, equalTo( "" ) );

        fullPath = "/api/content/npm/hosted/jkl";
        path = extractPath( fullPath );
        assertThat( path, equalTo( "" ) );
    }

    @Test
    public void testExtractPathFromLegacy()
    {
        String fullPath = "/api/group/abc/org/commonjava/indy/maven-metadata.xml";
        String path = extractPath( fullPath );
        assertThat( path, equalTo( "/org/commonjava/indy/maven-metadata.xml" ) );

        fullPath = "/api/group/builds-untested+shared-imports/org/commonjava/indy/maven-metadata.xml";
        path = extractPath( fullPath );
        assertThat( path, equalTo( "/org/commonjava/indy/maven-metadata.xml" ) );

        fullPath = "/api/hosted/def/org/commonjava/indy/1.0/";
        path = extractPath( fullPath );
        assertThat( path, equalTo( "/org/commonjava/indy/1.0/" ) );
    }

    @Test
    public void testProxyTo()
    {
        String fullPath = "/api/content/maven/group/abc/org/commonjava/indy/maven-metadata.xml";
        Optional<String> proxyTo = getProxyTo( fullPath, StoreKey.fromString( "maven:remote:group-abc" ) );
        assertTrue( proxyTo.isPresent() );
        assertThat( proxyTo.get(), equalTo( "/api/content/maven/remote/group-abc/org/commonjava/indy/maven-metadata.xml" ) );

        fullPath = "/api/content/maven/group/builds-untested+shared-imports/org/commonjava/indy/maven-metadata.xml";
        proxyTo = getProxyTo( fullPath, StoreKey.fromString( "maven:remote:group-builds-untested+shared-imports" ) );
        assertTrue( proxyTo.isPresent() );
        assertThat( proxyTo.get(), equalTo( "/api/content/maven/remote/group-builds-untested+shared-imports/org/commonjava/indy/maven-metadata.xml" ) );

        fullPath = "/api/browse/content/maven/hosted/def/org/commonjava/indy/1.0/";
        proxyTo = getProxyTo( fullPath, StoreKey.fromString( "maven:remote:hosted-def" ) );
        assertTrue( proxyTo.isPresent() );
        assertThat( proxyTo.get(), equalTo( "/api/browse/content/maven/remote/hosted-def/org/commonjava/indy/1.0/" ) );

        fullPath = "/api/folo/track/npm/group/ghi/jquery";
        proxyTo = getProxyTo( fullPath, StoreKey.fromString( "npm:remote:group-ghi" ) );
        assertTrue( proxyTo.isPresent() );
        assertThat( proxyTo.get(), equalTo( "/api/folo/track/npm/remote/group-ghi/jquery" ) );

        fullPath = "/api/folo/track/npm/hosted/jkl/@react/core";
        proxyTo = getProxyTo( fullPath, StoreKey.fromString( "npm:remote:hosted-jkl") );
        assertTrue( proxyTo.isPresent() );
        assertThat( proxyTo.get(), equalTo( "/api/folo/track/npm/remote/hosted-jkl/@react/core" ) );
    }

    @Test
    public void testProxyToFromLegacy()
    {
        String fullPath = "/api/group/abc/org/commonjava/indy/maven-metadata.xml";
        Optional<String> proxyTo = getProxyTo( fullPath, StoreKey.fromString( "maven:remote:group-abc" ) );
        assertTrue( proxyTo.isPresent() );
        assertThat( proxyTo.get(), equalTo( "/api/remote/group-abc/org/commonjava/indy/maven-metadata.xml" ) );

        fullPath = "/api/group/builds-untested+shared-imports/org/commonjava/indy/maven-metadata.xml";
        proxyTo = getProxyTo( fullPath, StoreKey.fromString( "maven:remote:group-builds-untested+shared-imports" ) );
        assertTrue( proxyTo.isPresent() );
        assertThat( proxyTo.get(), equalTo(
                "/api/remote/group-builds-untested+shared-imports/org/commonjava/indy/maven-metadata.xml" ) );
    }

}
