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
package org.commonjava.indy.mem.data;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.NoOpStoreEventDispatcher;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.infinispan.data.fixture.ThreadDumper;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.maven.galley.event.EventMetadata;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 10/21/16.
 */
@RunWith( BMUnitRunner.class )
@BMUnitConfig( debug = true )
public class ConcurrencyTest
{

    @Rule
    public TestRule timeout = ThreadDumper.timeoutRule( 10, TimeUnit.SECONDS );

    @BMRules( rules = { @BMRule( name = "init rendezvous", targetClass = "MemoryStoreDataManager",
                                 targetMethod = "<init>",
                                 targetLocation = "ENTRY",
                                 action = "createRendezvous($0, 2, true)" ),
            @BMRule( name = "getGroupsContaining call", targetClass = "MemoryStoreDataManager",
                     targetMethod = "getGroupsContaining",
                     targetLocation = "ENTRY", action = "rendezvous($0); debug(Thread.currentThread().getName() + \": thread proceeding.\")" ), } )
    @Test
    public void deadlockOnGroupContains()
            throws IndyDataException, InterruptedException, ExecutionException
    {
        ExecutorService executor = Executors.newFixedThreadPool( 2 );
        ExecutorCompletionService<String> completionService = new ExecutorCompletionService<>( executor );
        AtomicInteger count = new AtomicInteger( 0 );

        RemoteRepository repo = new RemoteRepository( MAVEN_PKG_KEY, "central", "http://repo.maven.apache.org/maven2" );

        TestUpdatingEventDispatcher dispatcher =
                new TestUpdatingEventDispatcher( repo, completionService, count );

        MemoryStoreDataManager dataManager =
                new MemoryStoreDataManager( dispatcher );

        dispatcher.setDataManager( dataManager );

        ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "Test init" );
        dataManager.storeArtifactStore( repo, summary, false, false, new EventMetadata() );

        for ( int i = 0; i < 2; i++ )
        {
            Group group = new Group( MAVEN_PKG_KEY, "group" + i );
            if ( i % 2 == 0 )
            {
                group.addConstituent( repo );
            }

            dataManager.storeArtifactStore( group, summary, false, false, new EventMetadata() );
        }

        for ( int i = 0; i < count.get(); i++ )
        {
            Future<String> future = completionService.take();
            assertThat( future.get(), nullValue() );
        }
    }

    @BMRules( rules = { @BMRule( name = "init rendezvous", targetClass = "MemoryStoreDataManager",
                                 targetMethod = "<init>",
                                 targetLocation = "ENTRY",
                                 action = "createRendezvous($0, 2)" ),
            @BMRule( name = "delete call", targetClass = "MemoryStoreDataManager",
                     targetMethod = "deleteArtifactStore",
                     targetLocation = "EXIT", action = "rendezvous($0); debug(Thread.currentThread().getName() + \": deletion thread proceeding.\")" ),
            @BMRule( name = "streamArtifactStores call", targetClass = "MemoryStoreDataManager",
                     targetMethod = "streamArtifactStores",
                     targetLocation = "ENTRY", action = "rendezvous($0); debug(Thread.currentThread().getName() + \": streamArtifactStores() thread proceeding.\")" ), } )
    @Test
    public void deadlockOnListAllDuringDelete()
            throws IndyDataException, InterruptedException, ExecutionException
    {
        ExecutorService executor = Executors.newFixedThreadPool( 2 );
        ExecutorCompletionService<String> completionService = new ExecutorCompletionService<>( executor );

        RemoteRepository repo = new RemoteRepository( MAVEN_PKG_KEY, "central", "http://repo.maven.apache.org/maven2" );

        TestDeletingEventDispatcher dispatcher =
                new TestDeletingEventDispatcher( completionService );

        MemoryStoreDataManager dataManager =
                new MemoryStoreDataManager( dispatcher );

        dispatcher.setDataManager( dataManager );

        ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "Test init" );
        dataManager.storeArtifactStore( repo, summary, false, false, new EventMetadata() );

        dataManager.deleteArtifactStore( repo.getKey(), new ChangeSummary( ChangeSummary.SYSTEM_USER, "Test deletion" ),
                                         new EventMetadata() );

        Future<String> future = completionService.take();
        assertThat( future.get(), nullValue() );
    }

    private static final class TestUpdatingEventDispatcher
            extends NoOpStoreEventDispatcher
    {
        static int counter = 0;

        private RemoteRepository repo;

        private final ExecutorCompletionService<String> completionService;

        private final int idx = counter++;

        private final AtomicInteger count;

        private StoreDataManager dataManager;

        public TestUpdatingEventDispatcher( RemoteRepository repo, ExecutorCompletionService<String> completionService,
                                            AtomicInteger count )
        {
            this.repo = repo;
            this.completionService = completionService;
            this.count = count;
        }

        @Override
        public void updating( ArtifactStoreUpdateType type, EventMetadata eventMetadata,
                              Map<ArtifactStore, ArtifactStore> stores )
        {
            for ( int i = 0; i < 2; i++ )
            {
                completionService.submit( () -> {
                    count.incrementAndGet();

                    Logger logger = LoggerFactory.getLogger( getClass() );
                    logger.debug( "Grabbing groups containing: {}", repo.getKey() );
                    try
                    {
                        if (!dataManager.query().packageType( MAVEN_PKG_KEY ).getGroupsContaining( repo.getKey() ).isEmpty())
                        {
                            return null;
                        }
                        else
                        {
                            return Thread.currentThread().getName() + "[execution: " + idx + "] cannot find any groups containing: " + repo.getKey();
                        }
                    }
                    catch ( IndyDataException e )
                    {
                        e.printStackTrace();
                    }

                    return "Failed to retrieve groups containing: " + repo.getKey();
                } );
            }
        }

        public void setDataManager( StoreDataManager dataManager )
        {
            this.dataManager = dataManager;
        }
    }

    private static final class TestDeletingEventDispatcher
            extends NoOpStoreEventDispatcher
    {
        private final ExecutorCompletionService<String> completionService;

        private StoreDataManager dataManager;

        public TestDeletingEventDispatcher( ExecutorCompletionService<String> completionService )
        {
            this.completionService = completionService;
        }

        @Override
        public void deleting( EventMetadata eventMetadata, ArtifactStore... stores )
        {
            completionService.submit( ()->{
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.debug( "Grabbing all artifact stores" );
                try
                {
                    dataManager.query().packageType( MAVEN_PKG_KEY ).getAllGroups();
                    return null;
                }
                catch ( IndyDataException e )
                {
                    e.printStackTrace();
                }

                return "Failed to list all artifact stores.";
            } );
        }

        public void setDataManager( StoreDataManager dataManager )
        {
            this.dataManager = dataManager;
        }
    }
}
