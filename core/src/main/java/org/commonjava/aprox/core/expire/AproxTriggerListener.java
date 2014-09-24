package org.commonjava.aprox.core.expire;

import javax.enterprise.event.Event;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerListener;

class AproxTriggerListener
    implements TriggerListener
{

    private final Event<SchedulerEvent> eventDispatcher;

    public AproxTriggerListener( final Event<SchedulerEvent> eventDispatcher )
    {
        this.eventDispatcher = eventDispatcher;
    }

    @Override
    public String getName()
    {
        return "AProx Triggers";
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