package org.commonjava.indy.schedule.datastax;

public enum JobType
{

    CONTENT ("CONTENT"),
    DisabledTIMEOUT ("Diabled-timeout");

    private final String jobType;

    private JobType( String jobType )
    {
        this.jobType = jobType;
    }

    public String getJobType()
    {
        return this.jobType;
    }

}
