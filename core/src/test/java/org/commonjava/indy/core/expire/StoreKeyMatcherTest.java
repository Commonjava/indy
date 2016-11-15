package org.commonjava.indy.core.expire;

import org.commonjava.indy.model.core.HostedRepository;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.util.Date;

import static org.commonjava.indy.core.expire.ScheduleManager.STORE_JOB_TYPE;
import static org.commonjava.indy.core.expire.ScheduleManager.groupName;
import static org.junit.Assert.assertTrue;

public class StoreKeyMatcherTest
{
    @Test
    public void testMatcher()
            throws Exception
    {
        final String hostedRepo = "hostedRepo";
        final HostedRepository repo = new HostedRepository( hostedRepo );
        final JobKey jk = new JobKey( STORE_JOB_TYPE, groupName( repo.getKey(), STORE_JOB_TYPE ) );

        JobDetail detail = JobBuilder.newJob( ExpirationJob.class )
                                     .withIdentity( jk )
                                     .storeDurably()
                                     .requestRecovery()
                                     .setJobData( new JobDataMap() )
                                     .build();
        final TriggerBuilder<Trigger> tb = TriggerBuilder.newTrigger()
                                                         .withIdentity( jk.getName(), jk.getGroup() )
                                                         .forJob( detail )
                                                         .startAt( new Date() );
        Trigger trigger = tb.build();

        StoreKeyMatcher skm = new StoreKeyMatcher( repo.getKey(), STORE_JOB_TYPE );
        assertTrue( skm.isMatch( trigger.getKey() ) );
    }
}
