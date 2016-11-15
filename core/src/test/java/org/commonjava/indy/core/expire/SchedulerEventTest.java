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
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;

import static org.commonjava.indy.core.expire.ScheduleManager.JOB_TYPE;
import static org.commonjava.indy.core.expire.ScheduleManager.PAYLOAD;
import static org.commonjava.indy.core.expire.ScheduleManager.STORE_JOB_TYPE;
import static org.commonjava.indy.core.expire.SchedulerEventType.TRIGGER;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SchedulerEventTest
{

    @Test
    public void testSchedulerEvent()
            throws Exception
    {
        final String hostedRepo = "hostedRepo";
        final HostedRepository repo = new HostedRepository( hostedRepo );
        final JobDataMap dataMap = new JobDataMap();
        final IndyObjectMapper mapper = new IndyObjectMapper( false );
        dataMap.put( JOB_TYPE, STORE_JOB_TYPE );
        dataMap.put( PAYLOAD, mapper.writeValueAsString( repo ) );

        JobDetail detail = JobBuilder.newJob( ExpirationJob.class )
                                     .withIdentity( STORE_JOB_TYPE )
                                     .storeDurably()
                                     .requestRecovery()
                                     .setJobData( dataMap )
                                     .build();

        final String type = detail.getJobDataMap().getString( JOB_TYPE );

        final String data = detail.getJobDataMap().getString( PAYLOAD );

        SchedulerEvent schedulerEvent = new SchedulerEvent( TRIGGER, type, data );

        assertThat( schedulerEvent, notNullValue() );
        assertThat( schedulerEvent.getJobType(), equalTo( STORE_JOB_TYPE ) );
        assertThat( schedulerEvent.getPayload(), equalTo( mapper.writeValueAsString( repo ) ) );
    }
}
