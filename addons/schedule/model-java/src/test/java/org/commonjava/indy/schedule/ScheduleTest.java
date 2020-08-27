package org.commonjava.indy.schedule;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.commonjava.indy.schedule.datastax.JobType;
import org.commonjava.indy.schedule.datastax.ScheduleDB;
import org.commonjava.indy.schedule.datastax.model.DtxSchedule;
import org.commonjava.indy.subsys.cassandra.CassandraClient;
import org.commonjava.indy.subsys.cassandra.config.CassandraConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ScheduleTest
{

    private static final String SCHEDULE_KEYSPACE = "schedule";

    ScheduleDB scheduleDB;

    @Before
    public void start() throws Exception
    {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();

        CassandraConfig config = new CassandraConfig();
        config.setEnabled( true );
        config.setCassandraHost( "localhost" );
        config.setCassandraPort( 9142 );

        CassandraClient client = new CassandraClient( config );
        scheduleDB = new ScheduleDB( client, SCHEDULE_KEYSPACE );
    }

    @After
    public void stop() throws Exception
    {
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }

    @Test
    public void test() throws Exception
    {
        Long timeout = Long.valueOf( 10 );

        scheduleDB.createSchedule( "maven:hosted:test", JobType.CONTENT.getJobType(), "org/jboss", timeout );

        Thread.sleep( timeout * 1000 );

        scheduleDB.queryTTLAndSetExpiredSchedule();

        Collection<DtxSchedule> schedules = scheduleDB.queryExpiredSchedule();

        assertThat( schedules.size(), equalTo( 1 ) );

        schedules.forEach( schedule -> {
            assertThat(schedule.getExpired(), equalTo(true));
        } );
    }


}
