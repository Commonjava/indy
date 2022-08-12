/*
 * Copyright (c) 2022 Red Hat, Inc
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
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.pkg.PackageTypeConstants;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static javax.ws.rs.HttpMethod.GET;
import static org.commonjava.indy.client.core.util.UrlUtils.normalizePath;
import static org.commonjava.indy.db.service.Utils.queryListingWithParamHandler;
import static org.commonjava.indy.db.service.Utils.queryWithParamHandler;
import static org.commonjava.indy.db.service.Utils.readResource;
import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class ServiceStoreQueryTest
{
    private static final String BASE_STORE_PATH = "/api/admin/stores";

    private static final String BASE_QUERY_PATH = BASE_STORE_PATH + "/query";

    private static CacheProducer producer;

    @Rule
    public ExpectationServer server = new ExpectationServer();

    private ServiceStoreQuery<ArtifactStore> query;

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
        ServiceStoreDataManager dataManager = new ServiceStoreDataManager( producer, client );
        query = new ServiceStoreQuery<>( dataManager, producer );
    }

    @Test
    public void testGetAll()
            throws Exception
    {
        String path = normalizePath( BASE_QUERY_PATH, "all/" );
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put( "packageType", "maven" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/group-static.json",
                                                                "repo-service/hosted-localdeploy.json",
                                                                "repo-service/hosted-readonly.json",
                                                                "repo-service/remote-central.json" ) );
        List<ArtifactStore> stores = query.getAll();
        assertThat( stores.size(), equalTo( 4 ) );

        queryParams.clear();
        queryParams.put( "packageType", "maven" );
        queryParams.put( "types", new HashSet<>( Arrays.asList( "remote", "hosted", "group" ) ) );
        queryParams.put( "enabled", "true" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/group-static.json",
                                                                "repo-service/hosted-localdeploy.json",
                                                                "repo-service/hosted-readonly.json",
                                                                "repo-service/remote-central.json" ) );
        stores = query.storeTypes( remote, hosted, group ).enabledState( true ).getAll();
        assertThat( stores.size(), equalTo( 4 ) );

        queryParams.clear();
        Set<String> typesSet = new HashSet<>( Arrays.asList( "remote", "group" ) );
        queryParams.put( "types", typesSet );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/group-static.json",
                                                                "repo-service/remote-central.json" ) );
        stores = query.noPackageType().storeTypes( remote, group ).getAll();
        assertThat( stores.size(), equalTo( 2 ) );

        queryParams.clear();
        typesSet = new HashSet<>( Arrays.asList( "remote", "hosted" ) );
        queryParams.put( "types", typesSet );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/hosted-localdeploy.json",
                                                                "repo-service/hosted-readonly.json",
                                                                "repo-service/remote-central.json" ) );
        stores = query.noPackageType().storeTypes( remote, hosted ).getAll();
        assertThat( stores.size(), equalTo( 3 ) );

    }

    @Test
    public void testGetSingles()
            throws Exception
    {
        String path = normalizePath( BASE_STORE_PATH, "maven/remote/central" );
        server.expect( path, 200, readResource( "repo-service/remote-central.json" ) );
        //                        Thread.sleep( 300000 );
        RemoteRepository remote = query.getRemoteRepository( PackageTypeConstants.PKG_TYPE_MAVEN, "central" );
        assertNotNull( remote );
        assertThat( remote.getKey(), equalTo( StoreKey.fromString( "maven:remote:central" ) ) );

        path = normalizePath( BASE_STORE_PATH, "maven/hosted/local-deployments" );
        server.expect( path, 200, readResource( "repo-service/hosted-localdeploy.json" ) );
        HostedRepository hosted = query.getHostedRepository( PackageTypeConstants.PKG_TYPE_MAVEN, "local-deployments" );
        assertNotNull( hosted );
        assertThat( hosted.getKey(), equalTo( StoreKey.fromString( "maven:hosted:local-deployments" ) ) );

        path = normalizePath( BASE_STORE_PATH, "maven/group/static" );
        server.expect( path, 200, readResource( "repo-service/group-static.json" ) );
        Group group = query.getGroup( PackageTypeConstants.PKG_TYPE_MAVEN, "static" );
        assertNotNull( group );
        assertThat( group.getKey(), equalTo( StoreKey.fromString( "maven:group:static" ) ) );
    }

    @Test
    public void testGroupContaining()
            throws Exception
    {
        final String path = normalizePath( BASE_QUERY_PATH, "groups/contains" );
        final Map<String, Object> queryParams = new HashMap<>();
        queryParams.put( "storeKey", "maven:remote:abc" );
        queryParams.put( "enabled", "true" );
        server.expect( GET, path, queryWithParamHandler( queryParams, "repo-service/listing-group-containing.json" ) );
        Set<Group> groups = query.getGroupsContaining( StoreKey.fromString( "maven:remote:abc" ), true );
        assertThat( groups.size(), equalTo( 2 ) );
    }

    @Test
    public void testGetGroupsAffectedBy()
            throws Exception
    {
        final String path = normalizePath( BASE_QUERY_PATH, "affectedBy/" );
        final Map<String, Object> queryParams = new HashMap<>();
        queryParams.put( "keys", new HashSet<>( Arrays.asList( "maven:remote:abc", "maven:hosted:def" ) ) );
        server.expect( GET, path, queryWithParamHandler( queryParams, "repo-service/listing-group-affectedBy.json" ) );
        Set<Group> groups = query.getGroupsAffectedBy( StoreKey.fromString( "maven:remote:abc" ),
                                                       StoreKey.fromString( "maven:hosted:def" ) );
        assertThat( groups.size(), equalTo( 3 ) );
    }

    @Test
    public void testGetRemoteRepositoryByUrl()
            throws Exception
    {
        final String path = normalizePath( BASE_QUERY_PATH, "remotes/" );
        final Map<String, Object> queryParams = new HashMap<>();
        queryParams.put( "packageType", "maven" );
        queryParams.put( "byUrl", "https://repo.maven.apache.org/maven2/" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-central.json" ) );
        List<RemoteRepository> remotes =
                query.getRemoteRepositoryByUrl( "maven", "https://repo.maven.apache.org/maven2/" );
        assertThat( remotes.size(), equalTo( 1 ) );
        assertThat( remotes.get( 0 ).getUrl(), equalTo( "https://repo.maven.apache.org/maven2/" ) );

        queryParams.clear();
        queryParams.put( "packageType", "npm" );
        queryParams.put( "byUrl", "https://registry.npmjs.org/" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-npm-npmjs.json" ) );
        remotes = query.getRemoteRepositoryByUrl( "npm", "https://registry.npmjs.org/" );
        assertThat( remotes.size(), equalTo( 1 ) );
        assertThat( remotes.get( 0 ).getUrl(), equalTo( "https://registry.npmjs.org/" ) );

        queryParams.clear();
        queryParams.put( "packageType", "maven" );
        queryParams.put( "byUrl", "https://repo.maven.apache.org/maven2/" );
        queryParams.put( "enabled", "false" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-central.json" ) );
        remotes = query.getRemoteRepositoryByUrl( "maven", "https://repo.maven.apache.org/maven2/", false );
        assertThat( remotes.size(), equalTo( 1 ) );
        assertThat( remotes.get( 0 ).getUrl(), equalTo( "https://repo.maven.apache.org/maven2/" ) );

        queryParams.clear();
        queryParams.put( "packageType", "npm" );
        queryParams.put( "byUrl", "https://registry.npmjs.org/" );
        queryParams.put( "enabled", "false" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-npm-npmjs.json" ) );
        remotes = query.getRemoteRepositoryByUrl( "npm", "https://registry.npmjs.org/", false );
        assertThat( remotes.size(), equalTo( 1 ) );
        assertThat( remotes.get( 0 ).getUrl(), equalTo( "https://registry.npmjs.org/" ) );
    }

    @Test
    public void testGetOrderedStoresInGroup()
            throws Exception
    {
        final String path = normalizePath( BASE_QUERY_PATH, "inGroup/" );
        final Map<String, Object> queryParams = new HashMap<>();
        queryParams.put( "storeKey", "maven:group:test" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-central.json",
                                                                "repo-service/hosted-readonly.json",
                                                                "repo-service/group-static.json" ) );
        List<ArtifactStore> stores = query.getOrderedStoresInGroup( "maven", "test" );
        assertThat( stores.size(), equalTo( 3 ) );

        queryParams.clear();
        queryParams.put( "storeKey", "npm:group:test" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-npm-npmjs.json" ) );
        stores = query.getOrderedStoresInGroup( "npm", "test" );
        assertThat( stores.size(), equalTo( 1 ) );

        queryParams.clear();
        queryParams.put( "storeKey", "maven:group:test" );
        queryParams.put( "enabled", "false" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-central.json",
                                                                "repo-service/group-static.json" ) );
        stores = query.getOrderedStoresInGroup( "maven", "test", false );
        assertThat( stores.size(), equalTo( 2 ) );

        queryParams.clear();
        queryParams.put( "storeKey", "npm:group:test" );
        queryParams.put( "enabled", "false" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-npm-npmjs.json" ) );
        stores = query.getOrderedStoresInGroup( "npm", "test", false );
        assertThat( stores.size(), equalTo( 1 ) );
    }

    @Test
    public void testGetOrderedConcreteStoresInGroup()
            throws Exception
    {
        final String path = normalizePath( BASE_QUERY_PATH, "concretes/inGroup/" );
        final Map<String, Object> queryParams = new HashMap<>();
        queryParams.put( "storeKey", "maven:group:test" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-central.json",
                                                                "repo-service/hosted-readonly.json" ) );
        List<ArtifactStore> stores = query.getOrderedConcreteStoresInGroup( "maven", "test" );
        assertThat( stores.size(), equalTo( 2 ) );

        queryParams.clear();
        queryParams.put( "storeKey", "npm:group:test" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-npm-npmjs.json" ) );
        stores = query.getOrderedConcreteStoresInGroup( "npm", "test" );
        assertThat( stores.size(), equalTo( 1 ) );

        queryParams.clear();
        queryParams.put( "storeKey", "maven:group:test" );
        queryParams.put( "enabled", "false" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-central.json" ) );
        stores = query.getOrderedConcreteStoresInGroup( "maven", "test", false );
        assertThat( stores.size(), equalTo( 1 ) );

        queryParams.clear();
        queryParams.put( "storeKey", "npm:group:test" );
        queryParams.put( "enabled", "false" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-npm-npmjs.json" ) );
        stores = query.getOrderedConcreteStoresInGroup( "npm", "test", false );
        assertThat( stores.size(), equalTo( 1 ) );
    }

    @Test
    public void testGetAllRemoteRepositories()
            throws Exception
    {
        final String path = normalizePath( BASE_QUERY_PATH, "remotes/all/" );
        final Map<String, Object> queryParams = new HashMap<>();
        queryParams.put( "packageType", "maven" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-central.json" ) );
        List<RemoteRepository> stores = query.getAllRemoteRepositories( "maven" );
        assertThat( stores.size(), equalTo( 1 ) );
        assertThat( stores.get( 0 ).getKey(), equalTo( StoreKey.fromString( "maven:remote:central" ) ) );

        queryParams.clear();
        queryParams.put( "packageType", "maven" );
        queryParams.put( "enabled", "true" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-central.json" ) );
        stores = query.getAllRemoteRepositories( "maven", true );
        assertThat( stores.size(), equalTo( 1 ) );
        assertThat( stores.get( 0 ).getKey(), equalTo( StoreKey.fromString( "maven:remote:central" ) ) );

        queryParams.clear();
        queryParams.put( "packageType", "npm" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-npm-npmjs.json" ) );
        stores = query.getAllRemoteRepositories( "npm" );
        assertThat( stores.size(), equalTo( 1 ) );
        assertThat( stores.get( 0 ).getKey(), equalTo( StoreKey.fromString( "npm:remote:npmjs" ) ) );

        queryParams.clear();
        queryParams.put( "packageType", "npm" );
        queryParams.put( "enabled", "true" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/remote-npm-npmjs.json" ) );
        stores = query.getAllRemoteRepositories( "npm", true );
        assertThat( stores.size(), equalTo( 1 ) );
        assertThat( stores.get( 0 ).getKey(), equalTo( StoreKey.fromString( "npm:remote:npmjs" ) ) );
    }

    @Test
    public void testGetAllHostedRepositories()
            throws Exception
    {
        final String path = normalizePath( BASE_QUERY_PATH, "hosteds/all/" );
        final Map<String, Object> queryParams = new HashMap<>();
        queryParams.put( "packageType", "maven" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/hosted-localdeploy.json",
                                                                "repo-service/hosted-readonly.json" ) );
        List<HostedRepository> stores = query.getAllHostedRepositories( "maven" );
        assertThat( stores.size(), equalTo( 2 ) );

        queryParams.clear();
        queryParams.put( "packageType", "maven" );
        queryParams.put( "enabled", "true" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/hosted-readonly.json" ) );
        stores = query.getAllHostedRepositories( "maven", true );
        assertThat( stores.size(), equalTo( 1 ) );
        assertThat( stores.get( 0 ).getKey(), equalTo( StoreKey.fromString( "maven:hosted:readonly" ) ) );

        queryParams.clear();
        queryParams.put( "packageType", "npm" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/hosted-npm-deploy.json" ) );
        stores = query.getAllHostedRepositories( "npm" );
        assertThat( stores.size(), equalTo( 1 ) );
        assertThat( stores.get( 0 ).getKey(), equalTo( StoreKey.fromString( "npm:hosted:deploy" ) ) );

        queryParams.clear();
        queryParams.put( "packageType", "npm" );
        queryParams.put( "enabled", "true" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/hosted-npm-deploy.json" ) );
        stores = query.getAllHostedRepositories( "npm", true );
        assertThat( stores.size(), equalTo( 1 ) );
        assertThat( stores.get( 0 ).getKey(), equalTo( StoreKey.fromString( "npm:hosted:deploy" ) ) );
    }

    @Test
    public void testGetAllGroups()
            throws Exception
    {
        final String path = normalizePath( BASE_QUERY_PATH, "groups/all/" );
        final Map<String, Object> queryParams = new HashMap<>();
        queryParams.put( "packageType", "maven" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/group-static.json" ) );
        List<Group> stores = query.getAllGroups( "maven" );
        assertThat( stores.size(), equalTo( 1 ) );
        assertThat( stores.get( 0 ).getKey(), equalTo( StoreKey.fromString( "maven:group:static" ) ) );

        queryParams.clear();
        queryParams.put( "packageType", "maven" );
        queryParams.put( "enabled", "true" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/group-static.json" ) );
        stores = query.getAllGroups( "maven", true );
        assertThat( stores.size(), equalTo( 1 ) );
        assertThat( stores.get( 0 ).getKey(), equalTo( StoreKey.fromString( "maven:group:static" ) ) );

        queryParams.clear();
        queryParams.put( "packageType", "npm" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/group-npm-public.json" ) );
        stores = query.getAllGroups( "npm" );
        assertThat( stores.size(), equalTo( 1 ) );
        assertThat( stores.get( 0 ).getKey(), equalTo( StoreKey.fromString( "npm:group:public" ) ) );

        queryParams.clear();
        queryParams.put( "packageType", "npm" );
        queryParams.put( "enabled", "true" );
        server.expect( GET, path, queryListingWithParamHandler( queryParams, "repo-service/group-npm-public.json" ) );
        stores = query.getAllGroups( "npm", true );
        assertThat( stores.size(), equalTo( 1 ) );
        assertThat( stores.get( 0 ).getKey(), equalTo( StoreKey.fromString( "npm:group:public" ) ) );
    }
}
