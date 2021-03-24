package org.commonjava.indy.schedule;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.schedule.conf.ScheduleDBConfig;
import org.commonjava.indy.schedule.datastax.JobType;
import org.commonjava.indy.schedule.datastax.model.DtxSchedule;
import org.commonjava.indy.subsys.cassandra.CassandraClient;
import org.commonjava.indy.subsys.cassandra.config.CassandraConfig;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
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

    CassandraClient client;

    @Before
    public void start() throws Exception
    {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();

        CassandraConfig config = new CassandraConfig();
        config.setEnabled( true );
        config.setCassandraHost( "localhost" );
        config.setCassandraPort( 9142 );

        client = new CassandraClient( config );
        ScheduleDBConfig scheduleDBConfig =
                        new ScheduleDBConfig( SCHEDULE_KEYSPACE, 1, 60 * 60 * 1000, 3 );

        DefaultIndyConfiguration indyConfig = new DefaultIndyConfiguration();
        indyConfig.setKeyspaceReplicas( 1 );

        EmbeddedCacheManager cacheManager = new DefaultCacheManager();
        scheduleDB = new ScheduleDB( indyConfig, scheduleDBConfig, client, new CacheProducer( null, cacheManager, null ) );
    }

    @After
    public void stop() throws Exception
    {
        client.close();
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }

    @Test
    public void testScheduleExpired() throws Exception
    {

        final String storeKey = "maven:hosted:test";
        final String jobName = "org/jboss";
        final String payload = "json_string";

        Long timeout = Long.valueOf( 10 );

        scheduleDB.createSchedule( storeKey, JobType.CONTENT.getJobType(), jobName, payload, timeout );

        Thread.sleep( 15 * 1000 );

        DtxSchedule schedule = scheduleDB.querySchedule( storeKey, jobName );

        assertThat( schedule.getExpired(), equalTo( true ) );

    }

    @Test
    public void testQueryDisabledTimeoutSchedule() throws Exception
    {
        final String storeKey = "maven:hosted:test";
        final String jobName = "org/jboss";
        final String payload = "json_string";

        scheduleDB.createSchedule( storeKey, JobType.DisabledTIMEOUT.getJobType(), jobName, payload, Long.valueOf( 20 ) );

        Collection<DtxSchedule> schedules = scheduleDB.querySchedulesByJobType( JobType.DisabledTIMEOUT.getJobType() );

        assertThat( schedules.size(), equalTo(1) );

    }

    @Test
    public void testReSchedule() throws Exception
    {
        final String storeKey = "maven:hosted:test";
        final String jobName = "org/jboss";
        final String payload = "json_string";

        scheduleDB.createSchedule( storeKey, JobType.CONTENT.getJobType(), jobName + "-01", payload, Long.valueOf( 10 ) );
        scheduleDB.createSchedule( storeKey, JobType.CONTENT.getJobType(), jobName + "-02", payload, Long.valueOf( 60 ) );
        scheduleDB.createSchedule( storeKey, JobType.CONTENT.getJobType(), jobName + "-03", payload, Long.valueOf( 20 ) );

        Thread.sleep( 10 * 1000 );

        final DtxSchedule schedule_01 = scheduleDB.querySchedule( storeKey, jobName + "-01" );

        // The schedule "-01" is expired after initial timeout 10 seconds
        assertThat( schedule_01.getExpired(), equalTo(Boolean.TRUE) );

        // Reschedule the "-03" and it should not expire after the initial 20 seconds
        scheduleDB.createSchedule( storeKey, JobType.CONTENT.getJobType(), jobName + "-03", payload, Long.valueOf( 60 ) );

        Thread.sleep( 15 * 1000 );

        final Collection<DtxSchedule> schedules = scheduleDB.querySchedules( storeKey, JobType.CONTENT.getJobType(), Boolean.TRUE );

        schedules.forEach( schedule -> {
            assertThat( schedules.size(), equalTo(1) );
        } );

        final Collection<DtxSchedule> schedules2 = scheduleDB.querySchedules( storeKey, JobType.CONTENT.getJobType(), Boolean.FALSE );

        schedules.forEach( schedule -> {
            assertThat( schedules2.size(), equalTo(2) );
        } );

        final DtxSchedule schedule_03 = scheduleDB.querySchedule( storeKey, jobName + "-03" );

        assertThat( schedule_03.getExpired(), equalTo(Boolean.FALSE) );
    }

}
