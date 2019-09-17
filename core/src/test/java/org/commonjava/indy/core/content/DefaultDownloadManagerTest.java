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
package org.commonjava.indy.core.content;

import org.commonjava.cdi.util.weft.PoolWeftExecutorService;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.content.ContentDigester;
import org.commonjava.indy.content.ContentGenerator;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.content.IndyLocationExpander;
import org.commonjava.indy.core.inject.ExpiringMemoryNotFoundCache;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.maven.galley.GalleyCore;
import org.commonjava.maven.galley.GalleyCoreBuilder;
import org.commonjava.maven.galley.GalleyInitException;
import org.commonjava.maven.galley.cache.FileCacheProviderFactory;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.SpecialPathManagerImpl;
import org.commonjava.maven.galley.io.checksum.TransferMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 12/2/16.
 */
public class DefaultDownloadManagerTest
{
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private DefaultDownloadManager downloadManager;

    private GalleyCore core;

    private StoreDataManager storeManager;

    private static DefaultCacheManager cacheManager;

    private static Cache<String, TransferMetadata> contentMetadata;

    private ContentManager contentManager;

    @BeforeClass
    public static void setupClass()
            throws IOException
    {
        cacheManager = new DefaultCacheManager(
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "infinispan-test.xml" ) );

        contentMetadata = cacheManager.getCache( "content-metadata", true );
    }


    @Before
    public void setup()
            throws IOException, GalleyInitException
    {
        contentMetadata.clear();

        core = new GalleyCoreBuilder( new FileCacheProviderFactory( temp.newFolder( "cache" ) ) ).build();

        storeManager = new MemoryStoreDataManager( true );

        LocationExpander locationExpander = new IndyLocationExpander( storeManager );

        final DefaultIndyConfiguration config = new DefaultIndyConfiguration();
        config.setNotFoundCacheTimeoutSeconds( 1 );
        final ExpiringMemoryNotFoundCache nfc = new ExpiringMemoryNotFoundCache( config );

        WeftExecutorService rescanService =
                        new PoolWeftExecutorService( "test-rescan-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );

        downloadManager = new DefaultDownloadManager( storeManager, core.getTransferManager(), locationExpander,null, nfc, rescanService);

        WeftExecutorService contentAccessService =
                        new PoolWeftExecutorService( "test-content-access-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );
        DirectContentAccess dca =
                        new DefaultDirectContentAccess( downloadManager, contentAccessService );

        ContentDigester contentDigester = new DefaultContentDigester( dca, new CacheHandle<String, TransferMetadata>(
                        "content-metadata", contentMetadata ) );

        contentManager = new DefaultContentManager( storeManager, downloadManager, new IndyObjectMapper( true ),
                                                    new SpecialPathManagerImpl(), new MemoryNotFoundCache(),
                                                    contentDigester, new ContentGeneratorManager() );
    }

    @Test
    public void getTransferFromStoreList_DownloadOp_SkipIfDoesntExist()
            throws IndyDataException, IndyWorkflowException
    {
        ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "Test setup" );
        HostedRepository hosted1 = new HostedRepository( MAVEN_PKG_KEY, "one" );
        HostedRepository hosted2 = new HostedRepository( MAVEN_PKG_KEY, "two" );

        storeManager.storeArtifactStore( hosted1, summary, false, true, new EventMetadata() );
        storeManager.storeArtifactStore( hosted2, summary, false, true, new EventMetadata() );

        String path = "/some/path.txt";

        Transfer transfer = downloadManager.getStorageReference( Arrays.asList( hosted1, hosted2 ), path,
                                                                         TransferOperation.DOWNLOAD );

        assertThat( transfer, nullValue() );
    }

    @Test
    public void getTransferFromEmptyStoreList_DownloadOp_SkipIfDoesntExist()
            throws IndyDataException, IndyWorkflowException
    {
        String path = "/some/path.txt";

        Transfer transfer = downloadManager.getStorageReference( Arrays.asList(), path,
                                                                 TransferOperation.DOWNLOAD );

        assertThat( transfer, nullValue() );
    }

    @Test( expected = IOException.class )
    public void getTransferFromNotAllowedDeletionStore_DownloadOp_ThrowException() throws Exception
    {
        ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "Test setup" );
        HostedRepository hosted = new HostedRepository( MAVEN_PKG_KEY, "one" );
        hosted.setReadonly( true );

        storeManager.storeArtifactStore( hosted, summary, false, true, new EventMetadata() );

        String originalString = "This is a test";
        final String path = "/path/path";

        Transfer transfer = downloadManager.getStorageReference( hosted, path, TransferOperation.DOWNLOAD );
        try(OutputStream out = transfer.openOutputStream( TransferOperation.UPLOAD ))
        {
            out.write( originalString.getBytes() );
        }

        assertThat( transfer.exists(), equalTo( true ) );

        transfer.delete();
    }
}
