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
package org.commonjava.indy.promote.data;

import org.apache.commons.io.IOUtils;
import org.commonjava.cdi.util.weft.Locker;
import org.commonjava.cdi.util.weft.PoolWeftExecutorService;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.content.ContentDigester;
import org.commonjava.indy.content.ContentGenerator;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.content.IndyLocationExpander;
import org.commonjava.indy.core.content.ContentGeneratorManager;
import org.commonjava.indy.core.content.DefaultContentDigester;
import org.commonjava.indy.core.content.DefaultContentManager;
import org.commonjava.indy.core.content.DefaultDirectContentAccess;
import org.commonjava.indy.core.content.DefaultDownloadManager;
import org.commonjava.indy.core.inject.ExpiringMemoryNotFoundCache;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.promote.conf.PromoteConfig;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.commonjava.indy.promote.validate.PromoteValidationsManager;
import org.commonjava.indy.promote.validate.PromotionValidationTools;
import org.commonjava.indy.promote.validate.PromotionValidator;
import org.commonjava.indy.promote.validate.ValidationRuleParser;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.datafile.change.DataFileEventManager;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.template.ScriptEngine;
import org.commonjava.indy.test.fixture.core.MockContentAdvisor;
import org.commonjava.indy.test.fixture.core.MockInstance;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.SpecialPathManagerImpl;
import org.commonjava.maven.galley.io.checksum.TransferMetadata;
import org.commonjava.maven.galley.maven.rel.MavenModelProcessor;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.commonjava.maven.galley.testing.maven.GalleyMavenFixture;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class PromotionManagerTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private ContentManager contentManager;

    private DownloadManager downloadManager;

    private StoreDataManager storeManager;

    private GalleyMavenFixture galleyParts;

    private PromotionManager manager;

    private DataFileManager dataManager;

    private PromoteValidationsManager validationsManager;

    private PromotionValidator validator;

    private Executor executor;

    private static final String FAKE_BASE_URL = "";

    private static DefaultCacheManager cacheManager;

    private static Cache<String, TransferMetadata> contentMetadata;

    private SpecialPathManager specialPathManager;

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

        galleyParts = new GalleyMavenFixture( true, temp );
        galleyParts.initMissingComponents();

        storeManager = new MemoryStoreDataManager( true );

        final DefaultIndyConfiguration indyConfig = new DefaultIndyConfiguration();
        indyConfig.setNotFoundCacheTimeoutSeconds( 1 );
        final ExpiringMemoryNotFoundCache nfc = new ExpiringMemoryNotFoundCache( indyConfig );

        WeftExecutorService rescanService =
                        new PoolWeftExecutorService( "test-rescan-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );

        downloadManager = new DefaultDownloadManager( storeManager, galleyParts.getTransferManager(),
                                                      new IndyLocationExpander( storeManager ),
                                                      new MockInstance<>( new MockContentAdvisor() ), nfc, rescanService );

        WeftExecutorService contentAccessService =
                        new PoolWeftExecutorService( "test-content-access-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );
        DirectContentAccess dca =
                new DefaultDirectContentAccess( downloadManager, contentAccessService );

        ContentDigester contentDigester = new DefaultContentDigester( dca, new CacheHandle<String, TransferMetadata>(
                "content-metadata", contentMetadata ) );

        specialPathManager = new SpecialPathManagerImpl();

        contentManager = new DefaultContentManager( storeManager, downloadManager, new IndyObjectMapper( true ),
                                                    specialPathManager, new MemoryNotFoundCache(),
                                                    contentDigester, new ContentGeneratorManager() );

        dataManager = new DataFileManager( temp.newFolder( "data" ), new DataFileEventManager() );
        validationsManager = new PromoteValidationsManager( dataManager, new PromoteConfig(),
                                                            new ValidationRuleParser( new ScriptEngine( dataManager ),
                                                                                      new IndyObjectMapper( true ) ) );

        WeftExecutorService validateService =
                        new PoolWeftExecutorService( "test-validate-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );
        MavenModelProcessor modelProcessor = new MavenModelProcessor();

        PromoteConfig config = new PromoteConfig();
        validator = new PromotionValidator( validationsManager,
                                            new PromotionValidationTools( contentManager, storeManager,
                                                                          galleyParts.getPomReader(),
                                                                          galleyParts.getMavenMetadataReader(),
                                                                          modelProcessor, galleyParts.getTypeMapper(),
                                                                          galleyParts.getTransferManager(),
                                                                          contentDigester, null, config ), storeManager, downloadManager, validateService, null );

        WeftExecutorService svc =
                new PoolWeftExecutorService( "test-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );

        manager =
                new PromotionManager( validator, contentManager, downloadManager, storeManager,
                                      new Locker<>(), config, nfc, svc, svc, specialPathManager );

        executor = Executors.newCachedThreadPool();
    }

    /**
     * On collision, the promotion manager should skip the second file to be promoted (instead of overwriting the
     * existing one). This assumes no overwrite attribute is available for setting in the promotion request (or that
     * it is available but isn't set...and defaults to false).
     * @throws Exception
     */
    @Test
    public void promoteAllByPath_CollidingPaths_VerifySecondSkipped()
            throws Exception
    {
        final HostedRepository source1 = new HostedRepository( MAVEN_PKG_KEY,  "source1" );
        final HostedRepository source2 = new HostedRepository( MAVEN_PKG_KEY,  "source2" );
        storeManager.storeArtifactStore( source1, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ),
                                         false, true, new EventMetadata() );
        storeManager.storeArtifactStore( source2, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ),
                                         false, true, new EventMetadata() );

        String originalString = "This is a test";

        final String path = "/path/path";
        contentManager.store( source1, path, new ByteArrayInputStream( originalString.getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );

        contentManager.store( source2, path, new ByteArrayInputStream( "This is another test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );

        final HostedRepository target = new HostedRepository( MAVEN_PKG_KEY,  "target" );
        storeManager.storeArtifactStore( target, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ),
                                         false, true, new EventMetadata() );

        PathsPromoteResult result =
                manager.promotePaths( new PathsPromoteRequest( source1.getKey(), target.getKey(), path ),
                                      FAKE_BASE_URL );

        assertThat( result.getRequest().getSource(), equalTo( source1.getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> skipped = result.getSkippedPaths();
        assertThat( skipped == null || skipped.isEmpty(), equalTo( true ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 1 ) );

        assertThat( result.getError(), nullValue() );

        Transfer ref = downloadManager.getStorageReference( target, path );
        assertThat( ref.exists(), equalTo( true ) );
        try (InputStream in = ref.openInputStream())
        {
            String value = IOUtils.toString( in );
            assertThat( value, equalTo( originalString ) );
        }

        result = manager.promotePaths(
                        new PathsPromoteRequest( source1.getKey(), target.getKey(), path ),
                        FAKE_BASE_URL );

        assertThat( result.getRequest().getSource(), equalTo( source1.getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        skipped = result.getSkippedPaths();
        assertThat( skipped, notNullValue() );
        assertThat( skipped.size(), equalTo( 1 ) );

        completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        assertThat( result.getError(), nullValue() );

        ref = downloadManager.getStorageReference( target, path );
        assertThat( ref.exists(), equalTo( true ) );
        try (InputStream in = ref.openInputStream())
        {
            String value = IOUtils.toString( in );
            assertThat( value, equalTo( originalString ) );
        }
    }

    @Test
    @Ignore( "volatile, owing to galley fs locks")
    public void promoteAllByPath_RaceToPromote_FirstLocksTargetStore()
            throws Exception
    {
        Random rand = new Random();
        final HostedRepository[] sources = { new HostedRepository( MAVEN_PKG_KEY,  "source1" ), new HostedRepository( MAVEN_PKG_KEY,  "source2" ) };
        final String[] paths = { "/path/path1", "/path/path2", "/path3", "/path/path/4" };
        Stream.of( sources ).forEach( ( source ) ->
                                      {
                                          try
                                          {
                                              storeManager.storeArtifactStore( source, new ChangeSummary(
                                                                                       ChangeSummary.SYSTEM_USER, "test setup" ), false, true,
                                                                               new EventMetadata() );

                                              Stream.of( paths ).forEach( ( path ) ->
                                                                          {
                                                                              byte[] buf = new byte[1024 * 1024 * 2];
                                                                              rand.nextBytes( buf );
                                                                              try
                                                                              {
                                                                                  contentManager.store( source, path,
                                                                                                        new ByteArrayInputStream(
                                                                                                                buf ),
                                                                                                        TransferOperation.UPLOAD,
                                                                                                        new EventMetadata() );
                                                                              }
                                                                              catch ( IndyWorkflowException e )
                                                                              {
                                                                                  e.printStackTrace();
                                                                                  Assert.fail(
                                                                                          "failed to store generated file to: "
                                                                                                  + source + path );
                                                                              }
                                                                          } );
                                          }
                                          catch ( IndyDataException e )
                                          {
                                              e.printStackTrace();
                                              Assert.fail( "failed to store hosted repository: " + source );
                                          }
                                      } );

        final HostedRepository target = new HostedRepository( MAVEN_PKG_KEY,  "target" );
        storeManager.storeArtifactStore( target, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ),
                                         false, true, new EventMetadata() );

        PathsPromoteResult[] results = new PathsPromoteResult[2];
        CountDownLatch cdl = new CountDownLatch( 2 );

        AtomicInteger counter = new AtomicInteger( 0 );
        Stream.of( sources ).forEach( ( source ) ->
                                      {
                                          int idx = counter.getAndIncrement();
                                          executor.execute( () ->
                                                            {
                                                                try
                                                                {
                                                                    results[idx] = manager.promotePaths(
                                                                            new PathsPromoteRequest( source.getKey(),
                                                                                                     target.getKey(),
                                                                                                     paths ),
                                                                            FAKE_BASE_URL );
                                                                }
                                                                catch ( Exception e )
                                                                {
                                                                    e.printStackTrace();
                                                                    Assert.fail( "Promotion from source: " + source
                                                                                         + " failed." );
                                                                }
                                                                finally
                                                                {
                                                                    cdl.countDown();
                                                                }
                                                            } );

                                          try
                                          {
                                              Thread.sleep( 25 );
                                          }
                                          catch ( InterruptedException e )
                                          {
                                              Assert.fail( "Test interrupted" );
                                          }
                                      } );

        assertThat( "Promotions failed to finish.", cdl.await( 30, TimeUnit.SECONDS ), equalTo( true ) );

        // first one should succeed.
        PathsPromoteResult result = results[0];
        assertThat( result.getRequest().getSource(), equalTo( sources[0].getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> skipped = result.getSkippedPaths();
        assertThat( skipped == null || skipped.isEmpty(), equalTo( true ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( paths.length ) );

        assertThat( result.getError(), nullValue() );

        Stream.of( paths ).forEach( ( path ) ->
                                    {
                                        HostedRepository src = sources[0];
                                        Transfer sourceRef = downloadManager.getStorageReference( src, path );
                                        Transfer targetRef = downloadManager.getStorageReference( target, path );
                                        assertThat( targetRef.exists(), equalTo( true ) );
                                        try (InputStream sourceIn = sourceRef.openInputStream();
                                             InputStream targetIn = targetRef.openInputStream())
                                        {
                                            int s = -1, t = -1;
                                            while ( ( s = sourceIn.read() ) == ( t = targetIn.read() ) )
                                            {
                                                if ( s == -1 )
                                                {
                                                    break;
                                                }
                                            }

                                            if ( s != -1 && s != t )
                                            {
                                                Assert.fail(
                                                        path + " doesn't match between source: " + src + " and target: "
                                                                + target );
                                            }
                                        }
                                        catch ( IOException e )
                                        {
                                            e.printStackTrace();
                                            Assert.fail(
                                                    "Failed to compare contents of: " + path + " between source: " + src
                                                            + " and target: " + target );
                                        }
                                    } );

        // second one should be completely skipped.
        result = results[1];
        assertThat( result.getRequest().getSource(), equalTo( sources[1].getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        skipped = result.getSkippedPaths();
        assertThat( skipped, notNullValue() );
        assertThat( skipped.size(), equalTo( paths.length ) );

        completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        assertThat( result.getError(), nullValue() );
    }

    @Test
    public void promoteAllByPath_PushTwoArtifactsToHostedRepo_VerifyCopiedToOtherHostedRepo()
            throws Exception
    {
        prepareHostedReposAndTwoPaths();

        final PathsPromoteResult result =
                manager.promotePaths( new PathsPromoteRequest( source.getKey(), target.getKey() ), FAKE_BASE_URL );

        assertThat( result.getRequest().getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        final Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        final Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        verifyExistence( true, true, true, true );
    }

    @Test
    public void promoteAllByPath_PushTwoArtifactsToHostedRepo_DryRun_VerifyPendingPathsPopulated()
            throws Exception
    {
        prepareHostedReposAndTwoPaths();

        final PathsPromoteResult result =
                manager.promotePaths( new PathsPromoteRequest( source.getKey(), target.getKey() ).setDryRun( true ),
                                      FAKE_BASE_URL );

        assertThat( result.getRequest().getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        final Set<String> completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        final Set<String> pending = result.getPendingPaths();
        assertThat( pending, notNullValue() );
        assertThat( pending.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        verifyExistence( false, false, true, true );
    }

    @Test
    public void promoteAllByPath_PurgeSource_PushTwoArtifactsToHostedRepo_VerifyCopiedToOtherHostedRepo()
            throws Exception
    {
        prepareHostedReposAndTwoPaths();

        final PathsPromoteResult result = manager.promotePaths(
                new PathsPromoteRequest( source.getKey(), target.getKey() ).setPurgeSource( true ), FAKE_BASE_URL );

        assertThat( result.getRequest().getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        final Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        final Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        verifyExistence( true, true, false, false );
    }

    @Test
    public void rollback_PushTwoArtifactsToHostedRepo_PromoteSuccessThenRollback()
            throws Exception
    {
        prepareHostedReposAndTwoPaths();

        PathsPromoteResult result =
                manager.promotePaths( new PathsPromoteRequest( source.getKey(), target.getKey() ), FAKE_BASE_URL );

        assertThat( result.getRequest().getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        Set<String> pending = result.getPendingPaths();
        assertThat( "should be null or empty: " + pending, pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        result = manager.rollbackPathsPromote( result );

        assertThat( result.getRequest().getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        pending = result.getPendingPaths();
        assertThat( pending, notNullValue() );
        assertThat( pending.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        verifyExistence( false, false, true, true );
    }

    @Test
    public void rollback_PurgeSource_PushTwoArtifactsToHostedRepo_PromoteSuccessThenRollback_VerifyContentInSource()
            throws Exception
    {
        prepareHostedReposAndTwoPaths();

        PathsPromoteResult result = manager.promotePaths(
                new PathsPromoteRequest( source.getKey(), target.getKey() ).setPurgeSource( true ), FAKE_BASE_URL );

        assertThat( result.getRequest().getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        result = manager.rollbackPathsPromote( result );

        assertThat( result.getRequest().getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        pending = result.getPendingPaths();
        assertThat( pending, notNullValue() );
        assertThat( pending.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        verifyExistence( false, false, true, true );
    }

    /**
     * To make the promotion fail, we just add a same path to target and set the request failWhenExists.
     */
    @Test
    public void rollback_PushTwoArtifactsToHostedRepo_PromoteFailedAndAutoRollback() throws Exception
    {
        prepareHostedReposAndTwoPaths();

        contentManager.store( target, second, new ByteArrayInputStream( "This is a test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );

        PathsPromoteRequest request = new PathsPromoteRequest( source.getKey(), target.getKey() );
        request.setFailWhenExists( true );

        PathsPromoteResult result = manager.promotePaths( request, FAKE_BASE_URL );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending, notNullValue() );
        assertThat( pending.size(), equalTo( 2 ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed.size(), equalTo( 0 ) );

        assertThat( result.getError(), notNullValue() );
        System.out.println( ">>> " + result.getError() );

        verifyExistence( false, true, true, true );
    }


    private HostedRepository source, target;

    private final String first = "/first/path";

    private final String second = "/second/path";

    private void prepareHostedReposAndTwoPaths() throws Exception
    {
        prepareHostedRepos();

        contentManager.store( source, first, new ByteArrayInputStream( "This is a test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );
        contentManager.store( source, second, new ByteArrayInputStream( "This is a test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );
    }

    private void prepareHostedRepos() throws Exception
    {
        source = new HostedRepository( MAVEN_PKG_KEY, "source" );
        storeManager.storeArtifactStore( source, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ), false,
                                         true, new EventMetadata() );
        target = new HostedRepository( MAVEN_PKG_KEY, "target" );
        storeManager.storeArtifactStore( target, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ), false,
                                         true, new EventMetadata() );
    }

    private void verifyExistence( boolean tgtFirst, boolean tgtSecond, boolean srcFirst, boolean srcSecond )
    {
        Transfer ref = downloadManager.getStorageReference( target, first );
        assertThat( ref.exists(), equalTo( tgtFirst ) );

        ref = downloadManager.getStorageReference( target, second );
        assertThat( ref.exists(), equalTo( tgtSecond ) );

        ref = downloadManager.getStorageReference( source, first );
        assertThat( ref.exists(), equalTo( srcFirst ) );

        ref = downloadManager.getStorageReference( source, second );
        assertThat( ref.exists(), equalTo( srcSecond ) );
    }
}
