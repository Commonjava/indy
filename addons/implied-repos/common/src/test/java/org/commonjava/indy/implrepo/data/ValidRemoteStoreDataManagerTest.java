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
package org.commonjava.indy.implrepo.data;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.implrepo.fixture.TestValidRemoteStoreDataManager;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.maven.galley.auth.MemoryPasswordManager;
import org.commonjava.maven.galley.cache.FileCacheProvider;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.event.NoOpFileEventManager;
import org.commonjava.maven.galley.io.HashedLocationPathGenerator;
import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.maven.GalleyMaven;
import org.commonjava.maven.galley.maven.GalleyMavenBuilder;
import org.commonjava.maven.galley.transport.htcli.HttpClientTransport;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ValidRemoteStoreDataManagerTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private StoreDataManager storeManager;

    private FileCacheProvider cache;

    private GalleyMaven galley;

    private File cacheDir;

    private ChangeSummary summary;

    @Before
    public void setup()
            throws Exception
    {
        cacheDir = temp.newFolder();
        cache = new FileCacheProvider( cacheDir, new HashedLocationPathGenerator(), new NoOpFileEventManager(),
                                       new NoOpTransferDecorator() );

        galley = new GalleyMavenBuilder( cache ).withEnabledTransports(
                new HttpClientTransport( new HttpImpl( new MemoryPasswordManager() ) ) ).build();

        storeManager = new TestValidRemoteStoreDataManager( galley.getTransferManager() );

        summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" );
    }

    @Test
    public void testRepoValidation()
            throws Exception
    {
        RemoteRepository validRepo = new RemoteRepository( "test", "http://www.foo.com" );
        assertTrue( storeManager.storeArtifactStore( validRepo, summary, new EventMetadata() ) );

        RemoteRepository inValidRepo = new RemoteRepository( "test", "this.is.not.valid.repo" );
        assertFalse( storeManager.storeArtifactStore( inValidRepo, summary, new EventMetadata() ) );

        Group group = new Group( "group" );
        assertTrue( storeManager.storeArtifactStore( group, summary, new EventMetadata() ) );

        HostedRepository hostedRepository = new HostedRepository( "hosted" );
        assertTrue( storeManager.storeArtifactStore( hostedRepository, summary, new EventMetadata() ) );
    }
}