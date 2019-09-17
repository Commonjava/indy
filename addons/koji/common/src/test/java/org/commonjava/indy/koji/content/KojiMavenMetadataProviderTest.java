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
package org.commonjava.indy.koji.content;

import com.redhat.red.build.koji.KojiClient;
import com.redhat.red.build.koji.KojiClientException;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.commonjava.cdi.util.weft.PoolWeftExecutorService;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.content.ContentDigester;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.content.IndyLocationExpander;
import org.commonjava.indy.core.content.DefaultContentDigester;
import org.commonjava.indy.core.content.DefaultDirectContentAccess;
import org.commonjava.indy.core.content.DefaultDownloadManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.koji.conf.IndyKojiConfig;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.atlas.maven.ident.ref.ProjectRef;
import org.commonjava.maven.galley.GalleyCore;
import org.commonjava.maven.galley.GalleyCoreBuilder;
import org.commonjava.maven.galley.GalleyInitException;
import org.commonjava.maven.galley.cache.FileCacheProviderFactory;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.checksum.TransferMetadata;
import org.commonjava.maven.galley.maven.internal.type.StandardTypeMapper;
import org.commonjava.maven.galley.transport.htcli.HttpClientTransport;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;
import org.commonjava.maven.galley.transport.htcli.conf.GlobalHttpConfiguration;
import org.commonjava.test.http.expect.ExpectationServer;
import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.hamcrest.CoreMatchers;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.commonjava.indy.koji.content.testutil.KojiMockHandlers.configureKojiServer;
import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 11/3/16.
 */
@Ignore
public class KojiMavenMetadataProviderTest
{
    private static final String KOJI_BASEPATH = "koji";

    private static final String VERIFY_BASEPATH = "verify";

    private static final String VERIFY_REPO = "verify-server";

    private AtomicInteger counter = new AtomicInteger( 0 );

    @Rule
    public TestName named = new TestName();

    @Rule
    public ExpectationServer server = new ExpectationServer();

    private CacheHandle<ProjectRef, Metadata> cache;

    private IndyKojiConfig kojiConfig;

    private KojiClient kojiClient;

    private DefaultCacheManager cacheMgr;

    private KojiMavenMetadataProvider provider;

    @Test
    public void excludeBinaryImportsFromVersionMetadata()
            throws Exception
    {
        initKojiClient( "metadata-with-import-generate", false );

        Metadata metadata =
                provider.getMetadata( new StoreKey( MAVEN_PKG_KEY,  group, "public" ), "commons-io/commons-io/maven-metadata.xml" );

        assertThat( metadata, notNullValue() );

        StringWriter sw = new StringWriter();
        new MetadataXpp3Writer().write( sw, metadata );
        System.out.println( sw.toString() );

        Versioning versioning = metadata.getVersioning();
        assertThat( versioning, notNullValue() );

        assertThat( versioning.getLatest(), equalTo( "2.4.0.redhat-1" ) );
        assertThat( versioning.getRelease(), equalTo( "2.4.0.redhat-1" ) );

        List<String> versions = versioning.getVersions();
        assertThat( versions, notNullValue() );
        assertThat( versions.size(), equalTo( 1 ) );

        int idx = 0;
        assertThat( versions.get( idx ), equalTo( "2.4.0.redhat-1" ) );

    }

    @Test
    public void retrieveVersionMetadata()
            throws Exception
    {
        initKojiClient( "simple-metadata-generate", false );

        Metadata metadata =
                provider.getMetadata( new StoreKey( MAVEN_PKG_KEY,  group, "public" ), "commons-io/commons-io/maven-metadata.xml" );

        assertThat( metadata, notNullValue() );

        StringWriter sw = new StringWriter();
        new MetadataXpp3Writer().write( sw, metadata );
        System.out.println( sw.toString() );

        Versioning versioning = metadata.getVersioning();
        assertThat( versioning, notNullValue() );

        assertThat( versioning.getLatest(), equalTo( "2.4.0.redhat-1" ) );
        assertThat( versioning.getRelease(), equalTo( "2.4.0.redhat-1" ) );

        List<String> versions = versioning.getVersions();
        assertThat( versions, notNullValue() );
        assertThat( versions.size(), equalTo( 2 ) );

        int idx = 0;
        assertThat( versions.get( idx ), equalTo( "2.1-redhat-1" ) );
        idx++;
        assertThat( versions.get( idx ), equalTo( "2.4.0.redhat-1" ) );

    }

    @Test
    public void retrieveVersionMetadataWithTagWhitelist()
            throws Exception
    {
        kojiConfig.setTagPatternsEnabled( true );
        kojiConfig.setTagPatterns( Collections.singletonList( "jb-.+" ) );

        initKojiClient( "whitelisted-tags-metadata-generate", false );

        Metadata metadata =
                provider.getMetadata( new StoreKey( MAVEN_PKG_KEY,  group, "public" ), "commons-io/commons-io/maven-metadata.xml" );

        assertThat( metadata, notNullValue() );

        StringWriter sw = new StringWriter();
        new MetadataXpp3Writer().write( sw, metadata );
        System.out.println( sw.toString() );

        Versioning versioning = metadata.getVersioning();
        assertThat( versioning, notNullValue() );

        assertThat( versioning.getLatest(), equalTo( "2.4.0.redhat-1" ) );
        assertThat( versioning.getRelease(), equalTo( "2.4.0.redhat-1" ) );

        List<String> versions = versioning.getVersions();
        assertThat( versions, notNullValue() );
        assertThat( versions.size(), equalTo( 2 ) );

        int idx = 0;
        assertThat( versions.get( idx ), equalTo( "2.1-redhat-1" ) );
        idx++;
        assertThat( versions.get( idx ), equalTo( "2.4.0.redhat-1" ) );

    }

    @Test
    public void retrieveVersionMetadataWithVerification()
            throws Exception
    {
        initKojiClient( "simple-metadata-verify", true );

        Metadata metadata =
                provider.getMetadata( new StoreKey( MAVEN_PKG_KEY,  group, "public" ), "commons-io/commons-io/maven-metadata.xml" );

        assertThat( metadata, notNullValue() );

        StringWriter sw = new StringWriter();
        new MetadataXpp3Writer().write( sw, metadata );
        System.out.println( sw.toString() );

        Versioning versioning = metadata.getVersioning();
        assertThat( versioning, notNullValue() );

        assertThat( versioning.getLatest(), equalTo( "2.4.0.redhat-1" ) );
        assertThat( versioning.getRelease(), equalTo( "2.4.0.redhat-1" ) );

        List<String> versions = versioning.getVersions();
        assertThat( versions, notNullValue() );
        assertThat( versions.size(), equalTo( 2 ) );

        int idx = 0;
        assertThat( versions.get( idx ), equalTo( "2.1-redhat-1" ) );
        idx++;
        assertThat( versions.get( idx ), equalTo( "2.4.0.redhat-1" ) );

    }

    @Test
    public void metadataNullWhenNoVersionsFound()
            throws Exception
    {
        initKojiClient( "no-metadata-generate", false );

        Metadata metadata =
                provider.getMetadata( new StoreKey( MAVEN_PKG_KEY,  group, "public" ), "commons-io/commons-io/maven-metadata.xml" );

        assertThat( metadata, nullValue() );
    }

    @Test
    public void allowVersionMetadataToExpire()
            throws Exception
    {
        initKojiClient( "simple-metadata-generate", false );

        StoreKey sk = new StoreKey( MAVEN_PKG_KEY,  group, "public" );
        String path = "commons-io/commons-io/maven-metadata.xml";

        Metadata metadata = provider.getMetadata( sk, path );

        assertThat( metadata, notNullValue() );

        StringWriter sw = new StringWriter();
        new MetadataXpp3Writer().write( sw, metadata );
        System.out.println( sw.toString() );

        Versioning versioning = metadata.getVersioning();
        assertThat( versioning, notNullValue() );

        assertThat( versioning.getLatest(), equalTo( "2.4.0.redhat-1" ) );
        assertThat( versioning.getRelease(), equalTo( "2.4.0.redhat-1" ) );

        List<String> versions = versioning.getVersions();
        assertThat( versions, notNullValue() );
        assertThat( versions.size(), equalTo( 2 ) );

        int idx = 0;
        assertThat( versions.get( idx ), equalTo( "2.1-redhat-1" ) );
        idx++;
        assertThat( versions.get( idx ), equalTo( "2.4.0.redhat-1" ) );

        String originalLastUpdated = versioning.getLastUpdated();

        Thread.sleep( 4000 );

        // reset to just after getAPIVersion, since this is only called when the client initializes.
        counter.set( 1 );

        metadata = provider.getMetadata( sk, path );

        assertThat( metadata, notNullValue() );

        sw = new StringWriter();
        new MetadataXpp3Writer().write( sw, metadata );
        System.out.println( sw.toString() );

        assertThat( metadata.getVersioning(), notNullValue() );

        System.out.printf( "\n\nOriginal lastUpdated: '%s'\nNew lastUpdated: '%s'\n\n", originalLastUpdated,
                           metadata.getVersioning().getLastUpdated() );
        assertThat( metadata.getVersioning().getLastUpdated(),
                    CoreMatchers.not( CoreMatchers.equalTo( originalLastUpdated ) ) );
    }

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private void initKojiClient( String exchangeName, boolean verifyArtifacts )
                    throws IOException, GalleyInitException, IndyDataException, KojiClientException
    {
        StoreDataManager storeDataManager = new MemoryStoreDataManager( true );

        if ( verifyArtifacts )
        {
            RemoteRepository verifyRepo = new RemoteRepository( MAVEN_PKG_KEY,  VERIFY_REPO, server.formatUrl( VERIFY_BASEPATH ) );

            storeDataManager.storeArtifactStore( verifyRepo, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                                "Adding verification repo" ), false,
                                                 true, new EventMetadata() );

            kojiConfig.setArtifactAuthorityStore( new StoreKey( MAVEN_PKG_KEY,  remote, VERIFY_REPO ).toString() );
        }

        String resourceBase = "koji-metadata/" + exchangeName;
        configureKojiServer( server, KOJI_BASEPATH, counter, resourceBase, verifyArtifacts, VERIFY_BASEPATH );
        kojiClient = new KojiClient( kojiConfig, new MemoryPasswordManager(), Executors.newCachedThreadPool() );

        GalleyCore galley = new GalleyCoreBuilder(
                new FileCacheProviderFactory( temp.newFolder( "cache" ) ) ).withEnabledTransports(
                new HttpClientTransport( new HttpImpl( new org.commonjava.maven.galley.auth.MemoryPasswordManager() ),
                                         new IndyObjectMapper( true ), new GlobalHttpConfiguration(), null, null ) ).build();

        WeftExecutorService rescanService =
                        new PoolWeftExecutorService( "test-rescan-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );

        DownloadManager downloadManager = new DefaultDownloadManager( storeDataManager, galley.getTransferManager(),
                                                                      new IndyLocationExpander( storeDataManager ), rescanService );

        WeftExecutorService contentAccessService =
                        new PoolWeftExecutorService( "test-content-access-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );

        DirectContentAccess directContentAccess = new DefaultDirectContentAccess( downloadManager, contentAccessService );

        DirectContentAccess dca =
                new DefaultDirectContentAccess( downloadManager, contentAccessService );

        ContentDigester contentDigester = new DefaultContentDigester( dca, new CacheHandle<String, TransferMetadata>(
                "content-metadata", contentMetadata ) );

        KojiBuildAuthority buildAuthority =
                new KojiBuildAuthority( kojiConfig, new StandardTypeMapper(), kojiClient, storeDataManager,
                                        contentDigester, directContentAccess, cacheManager );

        WeftExecutorService kojiMDService =
                        new PoolWeftExecutorService( "test-koji-metadata-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );

        provider = new KojiMavenMetadataProvider( this.cache, kojiClient, buildAuthority, kojiConfig, kojiMDService, cacheManager );
    }

    private static DefaultCacheManager cacheManager;

    private static Cache<String, TransferMetadata> contentMetadata;

    @BeforeClass
    public static void setupClass()
    {
        cacheManager = new DefaultCacheManager( new ConfigurationBuilder().simpleCache( true ).build() );

        contentMetadata = cacheManager.getCache( "content-metadata", true );

    }

    @Before
    public void setup()
            throws Exception
    {
        contentMetadata.clear();

        Thread.currentThread().setName( named.getMethodName() );
        cacheMgr = new DefaultCacheManager();
        String mdCacheName = "koji-maven-metadata";

        cache = new CacheHandle( mdCacheName, cacheMgr.getCache( mdCacheName, true ) );

        kojiConfig = new IndyKojiConfig();
        kojiConfig.setEnabled( true );
        kojiConfig.setLockTimeoutSeconds( 2 );
        kojiConfig.setMaxConnections( 2 );
        kojiConfig.setMetadataTimeoutSeconds( 2 );
        kojiConfig.setRequestTimeoutSeconds( 1 );
        kojiConfig.setStorageRootUrl( server.formatUrl( "kojiroot" ) );
        kojiConfig.setUrl( server.formatUrl( "koji" ) );

        kojiConfig.setTargetGroups( Collections.singletonMap( "public", "public" ) );
    }

    @After
    public void shutdown()
    {
        if ( kojiClient != null )
        {
            kojiClient.close();
        }

        if ( cacheMgr != null )
        {
            cacheMgr.stop();
        }
    }
}
