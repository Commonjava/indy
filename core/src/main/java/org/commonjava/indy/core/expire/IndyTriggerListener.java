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

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerListener;

class IndyTriggerListener
    implements TriggerListener
{

    private final Event<SchedulerEvent> eventDispatcher;

    public IndyTriggerListener( final Event<SchedulerEvent> eventDispatcher )
    {
        this.eventDispatcher = eventDispatcher;
    }

    @Override
    public String getName()
    {
        return "Indy Triggers";
    }

    @Override
    public void triggerFired( final Trigger trigger, final JobExecutionContext context )
    {
        eventDispatcher.fire( ScheduleManager.createEvent( SchedulerEventType.TRIGGER, context.getJobDetail() ) );
    }

    @Override
    public boolean vetoJobExecution( final Trigger trigger, final JobExecutionContext context )
    {
        return false;
    }

    @Override
    public void triggerMisfired( final Trigger trigger )
    {
    }

    @Override
    public void triggerComplete( final Trigger trigger, final JobExecutionContext context,
                                 final CompletedExecutionInstruction triggerInstructionCode )
    {
    }

}