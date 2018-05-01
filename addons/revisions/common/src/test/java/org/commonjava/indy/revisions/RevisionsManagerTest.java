/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.revisions;

import static org.apache.commons.lang.StringUtils.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.commonjava.indy.action.IndyLifecycleEventManager;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.revisions.conf.RevisionsConfig;
import org.commonjava.indy.revisions.testutil.TestProvider;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.datafile.change.DataFileEvent;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RevisionsManagerTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private RevisionsManager revManager;

    private RevisionsConfig config;

    private DataFileManager dfManager;

    private Weld weld;

    private WeldContainer container;

    private DataFileTestEventListener listener;

    private IndyLifecycleEventManager lcEvents;

    @Before
    public void setup()
    {
        TestProvider.setTemporaryFolder( temp );

        weld = new Weld();
        container = weld.initialize();

        dfManager = container.instance()
                             .select( DataFileManager.class )
                             .get();

        config = container.instance().select( RevisionsConfig.class ).get();
        config.setEnabled( true );

        revManager = container.instance()
                              .select( RevisionsManager.class )
                              .get();

        listener = container.instance()
                            .select( DataFileTestEventListener.class )
                            .get();

        lcEvents = container.instance()
                            .select( IndyLifecycleEventManager.class )
                            .get();
    }

    @Test
    public void commitTwoDifferentFilesAndRetrieveChangelogForOneOfThem()
        throws Exception
    {
        lcEvents.fireStarted();
        final DataFile f1 = dfManager.getDataFile( "test/foo.txt" );
        f1.writeString( "this is a test", "UTF-8", new ChangeSummary( "test-user", "test for first file." ) );

        final DataFile f2 = dfManager.getDataFile( "test/bar.txt" );
        f2.writeString( "this is a test", "UTF-8", new ChangeSummary( "test-user", "test for second file." ) );
        f2.writeString( "this is another test", "UTF-8", new ChangeSummary( "test-user", "test (2) for second file." ) );

        final List<DataFileEvent> events = listener.waitForEvents( 3 );
        System.out.println( "Got events:\n  " + join( events, "\n  " ) );

        final List<ChangeSummary> changeLog = revManager.getDataChangeLog( f2.getPath(), 0, -1 );
        assertThat( changeLog, notNullValue() );
        assertThat( changeLog.size(), equalTo( 2 ) );
    }

    @Test
    public void commitOneFileTwice_NoChangeSecondTime_RetrieveOneChangelog()
        throws Exception
    {
        lcEvents.fireStarted();
        revManager.setup();

        final DataFile f1 = dfManager.getDataFile( "test/foo.txt" );
        f1.writeString( "this is a test", "UTF-8", new ChangeSummary( "test-user", "test for first write of file." ) );

        List<DataFileEvent> events = listener.waitForEvents( 1 );
        System.out.println( "Got events:\n  " + join( events, "\n  " ) );

        f1.writeString( "this is a test", "UTF-8", new ChangeSummary( "test-user", "test for second write of file." ) );

        events = listener.waitForEvents( 1 );
        System.out.println( "Got events:\n  " + join( events, "\n  " ) );

        final List<ChangeSummary> changeLog = revManager.getDataChangeLog( f1.getPath(), 0, -1 );
        assertThat( changeLog, notNullValue() );
        assertThat( changeLog.size(), equalTo( 1 ) );

        assertThat( changeLog.get( 0 )
                             .getSummary()
                             .contains( "test for first write of file." ), equalTo( true ) );
    }

    @Test
    public void commitOneFileTwice_ChangedBeforeServerStart_FirstChangelogAppearsOnServerStart()
        throws Exception
    {
        revManager.setup();

        final DataFile f1 = dfManager.getDataFile( "test/foo.txt" );
        f1.writeString( "this is a test", "UTF-8", new ChangeSummary( "test-user", "test for first write of file." ) );

        List<DataFileEvent> events = listener.waitForEvents( 1 );
        System.out.println( "Got events:\n  " + join( events, "\n  " ) );

        f1.writeString( "this is another test", "UTF-8", new ChangeSummary( "test-user",
                                                                            "test for second write of file." ) );

        events = listener.waitForEvents( 1 );

        System.out.println( "Got events:\n  " + join( events, "\n  " ) );

        List<ChangeSummary> changeLog = revManager.getDataChangeLog( f1.getPath(), 0, -1 );
        assertThat( changeLog, notNullValue() );
        assertThat( changeLog.size(), equalTo( 0 ) );

        lcEvents.fireStarted();

        changeLog = revManager.getDataChangeLog( f1.getPath(), 0, -1 );
        assertThat( changeLog, notNullValue() );
        assertThat( changeLog.size(), equalTo( 1 ) );

        assertThat( changeLog.get( 0 )
                             .getSummary()
                             .contains( RevisionsManager.CATCHUP_CHANGELOG ), equalTo( true ) );
    }

    @Test
    public void commitTwoDifferentFilesAndRetrieveChangelogForOneOfThem_LimitToOldestEvent()
        throws Exception
    {
        lcEvents.fireStarted();
        final DataFile f1 = dfManager.getDataFile( "test/foo.txt" );
        f1.writeString( "this is a test", "UTF-8", new ChangeSummary( "test-user", "test for first file." ) );

        final DataFile f2 = dfManager.getDataFile( "test/bar.txt" );
        final String testSummary = "test for second file.";
        f2.writeString( "this is a test", "UTF-8", new ChangeSummary( "test-user", testSummary ) );
        f2.writeString( "this is another test", "UTF-8", new ChangeSummary( "test-user", "test (2) for second file." ) );

        listener.waitForEvents( 3 );

        final List<ChangeSummary> changeLog = revManager.getDataChangeLog( f2.getPath(), 1, 1 );
        assertThat( changeLog, notNullValue() );
        assertThat( changeLog.size(), equalTo( 1 ) );

        final ChangeSummary summary = changeLog.get( 0 );
        assertThat( summary.getSummary()
                           .startsWith( testSummary ), equalTo( true ) );
    }

    @ApplicationScoped
    static class DataFileTestEventListener
    {
        private final List<DataFileEvent> events = new ArrayList<>();

        public synchronized void event( @Observes final DataFileEvent event )
        {
            events.add( event );
            notifyAll();
        }

        public synchronized List<DataFileEvent> waitForEvents( final int size )
        {
            while ( events.size() < size )
            {
                try
                {
                    wait();
                }
                catch ( final InterruptedException e )
                {
                    return Collections.emptyList();
                }
            }

            final List<DataFileEvent> result = new ArrayList<>( events );
            events.clear();
            return result;
        }
    }

}
