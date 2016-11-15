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

public class EventTypeMatcherTest
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

        EventTypeMatcher etm = new EventTypeMatcher( STORE_JOB_TYPE );
        assertTrue( etm.isMatch( trigger.getKey() ) );
    }
}
