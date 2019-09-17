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
package org.commonjava.indy.subsys.git;

import org.apache.commons.io.FileUtils;
import org.commonjava.indy.audit.ChangeSummary;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.Assert.fail;

@RunWith( BMUnitRunner.class )
@BMUnitConfig( debug = true )
public class GitManagerConcurrentTest
        extends AbstractGitManagerTest
{

    private boolean failed;

    private File cloneDir;

    private GitManager git;

    @Before
    public void prepare()
            throws Exception
    {
        failed = false;
        final File root = unpackRepo( "test-indy-data.zip" );

        cloneDir = temp.newFolder();

        FileUtils.forceDelete( cloneDir );

        final String email = "me@nowhere.com";

        // NOTE: Leave off generation of file-list changed in commit message (third parameter, below)
        final GitConfig config =
                new GitConfig( cloneDir, root.toURI().toURL().toExternalForm(), false ).setUserEmail( email );
        git = new GitManager( config );
    }

    @BMRules( rules = { @BMRule( name = "init rendezvous", targetClass = "GitManager",
                                 targetMethod = "<init>",
                                 targetLocation = "ENTRY",
                                 action = "createRendezvous($0, 2, true)" ),
            @BMRule( name = "getHeadCommit call", targetClass = "GitManager",
                     targetMethod = "getHeadCommit(File)",
                     targetLocation = "ENTRY",
                     action = "debug(\"getHeadCommit() waiting...\"); rendezvous($0); debug(\"getHeadCommit(): thread proceeding.\")" ),
            @BMRule( name = "addAndCommitPaths call", targetClass = "GitManager",
                     targetMethod = "addAndCommitPaths(ChangeSummary,Collection)",
                     targetLocation = "ENTRY",
                     action = "debug(\"addAndCommitPaths() waiting...\"); rendezvous($0); debug(\"addAndCommitPaths(): thread proceeding.\")" ),
    } )
    @Test
    public void addToRepoWhileGettingHeadCommit()
            throws Exception
    {
        final int threshold = 2;
        final Executor pool = Executors.newFixedThreadPool( threshold );
        CountDownLatch latch = new CountDownLatch( threshold );

        final File f = new File( cloneDir, String.format( "test.txt" ) );
        FileUtils.write( f, "This is a test" );

        final String user = "testAddAndCommit";
        git.addFiles( new ChangeSummary( user, "first commit"), f );
        git.commit();

        pool.execute( () -> {
            try
            {
                git.getHeadCommit( f );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                failed = true;
            }
            finally
            {
                latch.countDown();
            }
        } );

        pool.execute( () -> {
            try
            {
                FileUtils.write( f, "This is another test" );
                git.addFiles( new ChangeSummary( user, "second commit"), f );
                git.commit();
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                failed = true;
            }
            finally
            {
                latch.countDown();
            }
        } );

        latch.await();
        if ( failed )
        {
            fail();
        }

    }

    @BMRules( rules = { @BMRule( name = "init rendezvous", targetClass = "GitManager",
                                 targetMethod = "<init>",
                                 targetLocation = "ENTRY",
                                 action = "createRendezvous($0, 2, true)" ),
            @BMRule( name = "addAndCommitPaths call", targetClass = "GitManager",
                     targetMethod = "addAndCommitPaths(ChangeSummary,Collection)",
                     targetLocation = "ENTRY",
                     action = "rendezvous($0); debug(Thread.currentThread().getName() + \": thread proceeding.\")" ), } )
    @Test
    public void concurrentAddToRepo()
            throws Exception
    {

        final int threshold = 2;
        final Executor pool = Executors.newFixedThreadPool( threshold );
        CountDownLatch latch = new CountDownLatch( threshold );

        for ( int i = 0; i < threshold; i++ )
        {
            final int j = i;
            pool.execute( () -> {
                try
                {
                    final File f = new File( cloneDir, String.format( "test%s.txt", j ) );
                    FileUtils.write( f, String.format( "This is a test %s", j ) );

                    final String user = "test" + j;
                    final String log = "test commit " + j;
                    git.addFiles( new ChangeSummary( user, log ), f );
                    git.commit();
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                    failed = true;
                }
                finally
                {
                    latch.countDown();
                }
            } );
        }

        latch.await();
        if ( failed )
        {
            fail();
        }

    }

    @BMRules( rules = { @BMRule( name = "init rendezvous", targetClass = "GitManager",
                                 targetMethod = "<init>",
                                 targetLocation = "ENTRY",
                                 action = "createRendezvous($0, 2, true)" ),
            @BMRule( name = "deleteAndCommitPaths call", targetClass = "GitManager",
                     targetMethod = "deleteAndCommitPaths(ChangeSummary,Collection)",
                     targetLocation = "ENTRY",
                     action = "rendezvous($0); debug(Thread.currentThread().getName() + \": thread proceeding.\")" ), } )
    @Test
    public void concurrentDeleteFromRepo()
            throws Exception
    {
        final int threshold = 2;
        for ( int i = 0; i < threshold; i++ )
        {
            try
            {
                final File f = new File( cloneDir, String.format( "test%s.txt", i ) );
                FileUtils.write( f, String.format( "This is a test %s", i ) );

                final String user = "test" + i;
                final String log = "test commit " + i;
                git.addFiles( new ChangeSummary( user, log ), f );
                git.commit();
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                failed = true;
            }
        }

        final Executor pool = Executors.newFixedThreadPool( threshold );
        CountDownLatch latch = new CountDownLatch( threshold );

        for ( int i = 0; i < threshold; i++ )
        {
            final int j = i;
            pool.execute( () -> {
                try
                {
                    final File f = new File( cloneDir, String.format( "test%s.txt", j ) );

                    final String user = "test" + j;
                    final String log = "delete test" + j + ".txt";
                    git.delete( new ChangeSummary( user, log ), f );
                    git.commit();
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                    failed = true;
                }
                finally
                {
                    latch.countDown();
                }
            } );
        }

        latch.await();
        if ( failed )
        {
            fail();
        }

    }

}
