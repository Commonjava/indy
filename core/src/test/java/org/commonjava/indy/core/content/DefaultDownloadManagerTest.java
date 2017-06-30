/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.content.IndyLocationExpander;
import org.commonjava.indy.core.inject.ExpiringMemoryNotFoundCache;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.maven.galley.GalleyCore;
import org.commonjava.maven.galley.GalleyCoreBuilder;
import org.commonjava.maven.galley.GalleyInitException;
import org.commonjava.maven.galley.cache.FileCacheProviderFactory;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Arrays;

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

    @Before
    public void setup()
            throws IOException, GalleyInitException
    {
        core = new GalleyCoreBuilder( new FileCacheProviderFactory( temp.newFolder( "cache" ) ) ).build();

        storeManager = new MemoryStoreDataManager( true );

        LocationExpander locationExpander = new IndyLocationExpander( storeManager );

        final DefaultIndyConfiguration config = new DefaultIndyConfiguration();
        config.setNotFoundCacheTimeoutSeconds( 1 );
        final ExpiringMemoryNotFoundCache nfc = new ExpiringMemoryNotFoundCache( config );

        downloadManager = new DefaultDownloadManager( storeManager, core.getTransferManager(), locationExpander,null, nfc);
    }

    @Test
    public void getTransferFromStoreList_DownloadOp_SkipIfDoesntExist()
            throws IndyDataException, IndyWorkflowException
    {
        ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "Test setup" );
        HostedRepository hosted1 = new HostedRepository( "one" );
        HostedRepository hosted2 = new HostedRepository( "two" );

        storeManager.storeArtifactStore( hosted1, summary );
        storeManager.storeArtifactStore( hosted2, summary );

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
}
