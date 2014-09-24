package org.commonjava.aprox.core.expire;

public class SchedulerEvent
{
    
    private final SchedulerEventType eventType;
    
    private final String jobType;
    
    private final String payload;
    
    public SchedulerEvent( final SchedulerEventType eventType, final String jobType, final String payload )
    {
        this.eventType = eventType;
        this.jobType = jobType;
        this.payload = payload;
    }

    public SchedulerEventType getEventType()
    {
        return eventType;
    }

    public String getJobType()
    {
        return jobType;
    }

    public String getPayload()
    {
        return payload;
    }

}
