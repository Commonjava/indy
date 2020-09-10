package org.commonjava.indy.schedule.event;

public class ScheduleTriggerEvent
{

    private final String jobType;

    private final String payload;

    public ScheduleTriggerEvent( final String jobType, final String payload )
    {
        this.jobType = jobType;
        this.payload = payload;
    }

    public String getJobType()
    {
        return jobType;
    }

    public String getPayload()
    {
        return payload;
    }

    @Override
    public String toString()
    {
        return String.format( "SchedulerTriggerEvent [eventType=%s, jobType=%s, payload=%s]", getClass().getName(), jobType, payload );
    }

}
