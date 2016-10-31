package org.commonjava.indy.mem.data;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.NoOpStoreEventDispatcher;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.maven.galley.event.EventMetadata;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Created by jdcasey on 10/21/16.
 */
@RunWith( BMUnitRunner.class )
@BMUnitConfig( debug = true )
public class ConcurrencyTest
{

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

        RemoteRepository repo = new RemoteRepository( "central", "http://repo.maven.apache.org/maven2" );

        TestStoreEventDispatcher dispatcher =
                new TestStoreEventDispatcher( repo, completionService, count );

        MemoryStoreDataManager dataManager =
                new MemoryStoreDataManager( dispatcher,
                                            new DefaultIndyConfiguration() );

        dispatcher.setDataManager( dataManager );

        ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "Test init" );
        dataManager.storeArtifactStore( repo, summary );

        for ( int i = 0; i < 2; i++ )
        {
            Group group = new Group( "group" + i );
            if ( i % 2 == 0 )
            {
                group.addConstituent( repo );
            }

            dataManager.storeArtifactStore( group, summary );
        }

        for ( int i = 0; i < count.get(); i++ )
        {
            Future<String> future = completionService.take();
            assertThat( future.get(), nullValue() );
        }
    }

    private static final class TestStoreEventDispatcher
            extends NoOpStoreEventDispatcher
    {
        static int counter = 0;

        private RemoteRepository repo;

        private final ExecutorCompletionService<String> completionService;

        private final int idx = counter++;

        private final AtomicInteger count;

        private StoreDataManager dataManager;

        public TestStoreEventDispatcher( RemoteRepository repo, ExecutorCompletionService<String> completionService,
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
                        if (!dataManager.getGroupsContaining( repo.getKey() ).isEmpty())
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
}
