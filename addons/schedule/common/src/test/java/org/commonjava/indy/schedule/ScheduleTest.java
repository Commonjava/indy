package org.commonjava.indy.schedule;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.commonjava.indy.schedule.conf.ScheduleDBConfig;
import org.commonjava.indy.schedule.datastax.JobType;
import org.commonjava.indy.schedule.datastax.model.DtxSchedule;
import org.commonjava.indy.subsys.cassandra.CassandraClient;
import org.commonjava.indy.subsys.cassandra.config.CassandraConfig;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

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
        ScheduleDBConfig scheduleDBConfig =
                        new ScheduleDBConfig( SCHEDULE_KEYSPACE, 1, Long.valueOf( 60 * 60 * 1000 ) );
        scheduleDB = new ScheduleDB( scheduleDBConfig, client );
    }

    @After
    public void stop() throws Exception
    {
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }

    @Test
    public void test() throws Exception
    {

        final String storeKey = "maven:hosted:test";
        final String jobName = "org/jboss";

        Long timeout = Long.valueOf( 10 );

        scheduleDB.createSchedule(storeKey,JobType.CONTENT.getJobType(),jobName,timeout );

        Thread.sleep( timeout * 1000 );

        scheduleDB.queryTTLAndSetExpiredSchedule( new Date(  ) );

        DtxSchedule schedule = scheduleDB.querySchedule( storeKey, jobName );

        assertThat( schedule.getExpired(), Matchers.equalTo( true ) );

    }


}
