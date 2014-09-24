package org.commonjava.aprox.revisions;

import static org.apache.commons.lang.StringUtils.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.revisions.testutil.TestProvider;
import org.commonjava.aprox.subsys.datafile.DataFile;
import org.commonjava.aprox.subsys.datafile.DataFileManager;
import org.commonjava.aprox.subsys.datafile.change.DataFileEvent;
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

    private DataFileManager dfManager;

    private Weld weld;

    private WeldContainer container;

    private DataFileTestEventListener listener;

    @Before
    public void setup()
    {
        TestProvider.setTemporaryFolder( temp );

        weld = new Weld();
        container = weld.initialize();

        dfManager = container.instance()
                             .select( DataFileManager.class )
                             .get();

        revManager = container.instance()
                              .select( RevisionsManager.class )
                              .get();

        listener = container.instance()
                            .select( DataFileTestEventListener.class )
                            .get();
    }

    @Test
    public void commitTwoDifferentFilesAndRetrieveChangelogForOneOfThem()
        throws Exception
    {
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
    public void commitTwoDifferentFilesAndRetrieveChangelogForOneOfThem_LimitToOldestEvent()
        throws Exception
    {
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
