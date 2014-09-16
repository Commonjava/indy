package org.commonjava.aprox.audit;

import java.util.Date;

public class ChangeSummary
{
    public static final String SYSTEM_USER = "system";

    private final String user;

    private final Date timestamp;

    private final String summary;

    public ChangeSummary( final String user, final String summary )
    {
        this.user = user;
        this.summary = summary;
        this.timestamp = new Date();
    }

    public ChangeSummary( final String user, final String summary, final Date timestamp )
    {
        this.user = user;
        this.summary = summary;
        this.timestamp = timestamp;
    }

    public String getUser()
    {
        return user;
    }

    public String getSummary()
    {
        return summary;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    @Override
    public String toString()
    {
        return String.format( "[%s; %s] %s", user, timestamp, summary );
    }

}
