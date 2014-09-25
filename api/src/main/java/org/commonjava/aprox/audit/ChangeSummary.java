package org.commonjava.aprox.audit;

import java.util.Date;

public class ChangeSummary
{
    public static final String SYSTEM_USER = "system";

    private final String user;

    private final Date timestamp;

    private final String summary;

    private final String revisionId;

    public ChangeSummary( final String user, final String summary )
    {
        this.user = user;
        this.summary = summary;
        this.timestamp = new Date();
        this.revisionId = null;
    }

    public ChangeSummary( final String user, final String summary, final Date timestamp, final String revisionId )
    {
        this.user = user;
        this.summary = summary;
        this.timestamp = timestamp;
        this.revisionId = revisionId;
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

    public String getRevisionId()
    {
        return revisionId;
    }

}
