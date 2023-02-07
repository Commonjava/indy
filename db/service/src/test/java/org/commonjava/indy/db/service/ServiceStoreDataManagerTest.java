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
package org.commonjava.indy.db.service;

import org.commonjava.indy.client.core.Indy;
import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.dto.SimpleBooleanResultDTO;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.commonjava.indy.subsys.service.IndyClientProducer;
import org.commonjava.indy.subsys.service.config.RepositoryServiceConfig;
import org.commonjava.test.http.expect.ExpectationServer;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.ws.rs.HttpMethod.GET;
import static org.commonjava.indy.client.core.util.UrlUtils.normalizePath;
import static org.commonjava.indy.db.service.Utils.queryListingWithParamHandler;
import static org.commonjava.indy.db.service.Utils.queryWithParamHandler;
import static org.commonjava.indy.db.service.Utils.readResource;
import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ServiceStoreDataManagerTest
{
    private static final String BASE_STORE_PATH = "/api/admin/stores";

    private static final String BASE_QUERY_PATH = BASE_STORE_PATH + "/query";

    private static CacheProducer producer;

    @Rule
    public ExpectationServer server = new ExpectationServer();

    private ServiceStoreDataManager dataManager;

    private final IndyObjectMapper mapper = new IndyObjectMapper( false );

    @BeforeClass
    public static void setUpAll()
    {
        DefaultCacheManager cacheManager =
                new DefaultCacheManager( new ConfigurationBuilder().simpleCache( true ).build() );
        producer = new CacheProducer( new DefaultIndyConfiguration(), cacheManager, null );
        producer.start();
    }

    @Before
    public void setUp()
    {
        RepositoryServiceConfig serviceConfig = new RepositoryServiceConfig();
        System.out.println( server.getBaseUri() );
        serviceConfig.setEnabled( true );
        serviceConfig.setServiceUrl( UrlUtils.buildUrl( server.getBaseUri(), "api" ) );
        IndyClientProducer clientProducer = new IndyClientProducer( serviceConfig );
        clientProducer.init();
        Indy client = clientProducer.getClient();
        dataManager = new ServiceStoreDataManager( producer, client );
    }

    @Test
    public void testGetSingles()
            throws Exception
    {
        String path = normalizePath( BASE_STORE_PATH, "maven/remote/central" );
        server.expect( path, 200, readResource( "repo-service/remote-central.json" ) );
        StoreKey key = StoreKey.fromString( "maven:remote:central" );
        ArtifactStore remote = dataManager.getArtifactStore( key );
        assertNotNull( remote );
        assertThat( remote.getKey(), equalTo( key ) );
        assertThat( remote.getType(), equalTo( StoreType.remote ) );

        path = normalizePath( BASE_STORE_PATH, "maven/hosted/local-deployments" );
        server.expect( path, 200, readResource( "repo-service/hosted-localdeploy.json" ) );
        key = StoreKey.fromString( "maven:hosted:local-deployments" );
        ArtifactStore hosted = dataManager.getArtifactStore( key );
        assertNotNull( hosted );
        assertThat( hosted.getKey(), equalTo( key ) );
        assertThat( hosted.getType(), equalTo( StoreType.hosted ) );

        path = normalizePath( BASE_STORE_PATH, "maven/group/static" );
        server.expect( path, 200, readResource( "repo-service/group-static.json" ) );
        key = StoreKey.fromString( "maven:group:static" );
        ArtifactStore group = dataManager.getArtifactStore( key );
        assertNotNull( group );
        assertThat( group.getKey(), equalTo( key ) );
        assertThat( group.getType(), equalTo( StoreType.group ) );
    }

    @Test
    public void testIsEmpty()
            throws Exception
    {
        String path = normalizePath( BASE_QUERY_PATH, "isEmpty" );
        SimpleBooleanResultDTO dto = new SimpleBooleanResultDTO();
        dto.setDescription( "This is for testing" );
        dto.setResult( true );
        server.expect( path, 200, mapper.writeValueAsString( dto ) );
        assertTrue( dataManager.isEmpty() );
        dto.setResult( false );
        server.expect( path, 200, mapper.writeValueAsString( dto ) );
        assertFalse( dataManager.isEmpty() );
    }

    @Test
    public void testHasArtifactstore()
            throws Exception
    {
        StoreKey key = StoreKey.fromString( "maven:remote:notexist" );
        assertFalse( dataManager.hasArtifactStore( key ) );
        key = StoreKey.fromString( "maven:remote:central" );
        String path = normalizePath( BASE_STORE_PATH, "maven/remote/central" );
        server.expect( path, 200, readResource( "repo-service/remote-central.json" ) );
        assertTrue( dataManager.hasArtifactStore( key ) );
    }

    @Test
    public void testIsReadonly()
            throws Exception
    {
        String path = normalizePath( BASE_STORE_PATH, "maven/hosted/local-deployments" );
        server.expect( path, 200, readResource( "repo-service/hosted-localdeploy.json" ) );
        StoreKey key = StoreKey.fromString( "maven:hosted:local-deployments" );
        ArtifactStore hosted = dataManager.getArtifactStore( key );
        assertFalse( dataManager.isReadonly( hosted ) );
        path = normalizePath( BASE_STORE_PATH, "maven/hosted/readonly" );
        server.expect( path, 200, readResource( "repo-service/hosted-readonly.json" ) );
        key = StoreKey.fromString( "maven:hosted:readonly" );
        hosted = dataManager.getArtifactStore( key );
        assertTrue( dataManager.isReadonly( hosted ) );
    }

    @Test
    public void testGetArtifactStoresByPkgAndType()
            throws Exception
    {
        String path = normalizePath( BASE_QUERY_PATH, "all/" );
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put( "packageType", "maven" );
        queryParams.put( "types", "remote" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-central.json" ) );
        Set<ArtifactStore> stores = dataManager.getArtifactStoresByPkgAndType( "maven", StoreType.remote );
        assertThat( stores.size(), equalTo( 1 ) );
        assertThat( new ArrayList<>( stores ).get( 0 ).getKey(),
                    equalTo( StoreKey.fromString( "maven:remote:central" ) ) );

        queryParams.clear();
        queryParams.put( "packageType", "maven" );
        queryParams.put( "types", "hosted" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/hosted-localdeploy.json",
                                                                "repo-service/hosted-readonly.json" ) );
        stores = dataManager.getArtifactStoresByPkgAndType( "maven", hosted );
        assertThat( stores.size(), equalTo( 2 ) );
        assertThat( stores.stream().filter( s -> s.getType() == hosted ).collect( Collectors.toSet() ).size(),
                    equalTo( 2 ) );

        queryParams.clear();
        queryParams.put( "packageType", "maven" );
        queryParams.put( "types", "group" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/group-static.json" ) );
        stores = dataManager.getArtifactStoresByPkgAndType( "maven", group );
        assertThat( stores.size(), equalTo( 1 ) );
        assertThat( new ArrayList<>( stores ).get( 0 ).getKey(),
                    equalTo( StoreKey.fromString( "maven:group:static" ) ) );

    }

    @Test
    public void testGetAllArtifactStores()
            throws Exception
    {
        String path = normalizePath( BASE_QUERY_PATH, "all/" );
        Map<String, Object> queryParams = new HashMap<>();
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/group-static.json",
                                                                "repo-service/hosted-localdeploy.json",
                                                                "repo-service/hosted-readonly.json",
                                                                "repo-service/remote-central.json" ) );
        Set<ArtifactStore> stores = dataManager.getAllArtifactStores();
        assertThat( stores.size(), equalTo( 4 ) );
        assertThat( dataManager.streamArtifactStores().count(), equalTo( 4L ) );
        assertThat( dataManager.streamArtifactStoreKeys().count(), equalTo( 4L ) );
    }

    @Test
    public void testAffectedBy()
            throws Exception
    {
        final String path = normalizePath( BASE_QUERY_PATH, "affectedBy/" );
        final Map<String, Object> queryParams = new HashMap<>();
        queryParams.put( "keys", new HashSet<>( Arrays.asList( "maven:remote:abc", "maven:hosted:def" ) ) );
        server.expect( GET, path, queryWithParamHandler( queryParams, "repo-service/listing-group-affectedBy.json" ) );
        Set<Group> groups = dataManager.affectedBy(
                Arrays.asList( StoreKey.fromString( "maven:remote:abc" ), StoreKey.fromString( "maven:hosted:def" ) ) );
        assertThat( groups.size(), equalTo( 3 ) );
    }

}
