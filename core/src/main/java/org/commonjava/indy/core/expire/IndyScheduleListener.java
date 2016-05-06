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

import javax.enterprise.event.Event;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class IndyScheduleListener
    implements SchedulerListener
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Event<SchedulerEvent> eventDispatcher;

    private final Scheduler scheduler;

    public IndyScheduleListener( final Scheduler scheduler, final Event<SchedulerEvent> eventDispatcher )
    {
        this.scheduler = scheduler;
        this.eventDispatcher = eventDispatcher;
    }

    @Override
    public void jobScheduled( final Trigger trigger )
    {
        if ( trigger == null )
        {
            return;
        }

        final JobKey key = trigger.getJobKey();
        try
        {
            final JobDetail detail = scheduler.getJobDetail( key );
            eventDispatcher.fire( ScheduleManager.createEvent( SchedulerEventType.SCHEDULE, detail ) );
        }
        catch ( final SchedulerException e )
        {
            logger.error( "Cannot find scheduler job for key: " + key, e );
        }
    }

    @Override
    public void jobUnscheduled( final TriggerKey triggerKey )
    {
        try
        {
            EventTypeMatcher etm = new EventTypeMatcher( ScheduleManager.CONTENT_JOB_TYPE );
            if ( etm.isMatch( triggerKey ) )
            {
                // for CONTENT job type we use the same group and name for both job and trigger
                JobKey jobKey = new JobKey( triggerKey.getName(), triggerKey.getGroup() );
                final JobDetail detail = scheduler.getJobDetail( jobKey );
                if ( detail == null )
                {
                    logger.error( "Cannot find scheduler job for key: " + jobKey );
                }
                else
                {
                    eventDispatcher.fire( ScheduleManager.createEvent( SchedulerEventType.CANCEL, detail ) );
                }
            } else
            {
                logger.error( "Cannot produce job key for trigger key: {}. Unknown job type." + triggerKey );
            }
        }
        catch ( final SchedulerException e )
        {
            logger.error( "Cannot find scheduler job for key: " + triggerKey, e );
        }
    }

    @Override
    public void triggerFinalized( final Trigger trigger )
    {
    }

    @Override
    public void triggerPaused( final TriggerKey triggerKey )
    {
    }

    @Override
    public void triggersPaused( final String triggerGroup )
    {
    }

    @Override
    public void triggerResumed( final TriggerKey triggerKey )
    {
    }

    @Override
    public void triggersResumed( final String triggerGroup )
    {
    }

    @Override
    public void jobAdded( final JobDetail jobDetail )
    {
    }

    @Override
    public void jobDeleted( final JobKey jobKey )
    {
    }

    @Override
    public void jobPaused( final JobKey jobKey )
    {
    }

    @Override
    public void jobsPaused( final String jobGroup )
    {
    }

    @Override
    public void jobResumed( final JobKey jobKey )
    {
    }

    @Override
    public void jobsResumed( final String jobGroup )
    {
    }

    @Override
    public void schedulerError( final String msg, final SchedulerException cause )
    {
    }

    @Override
    public void schedulerInStandbyMode()
    {
    }

    @Override
    public void schedulerStarted()
    {
    }

    @Override
    public void schedulerStarting()
    {
    }

    @Override
    public void schedulerShutdown()
    {
    }

    @Override
    public void schedulerShuttingdown()
    {
    }

    @Override
    public void schedulingDataCleared()
    {
    }
}
