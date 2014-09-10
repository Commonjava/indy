package org.commonjava.aprox.audit;

public class ChangeSummary
{
    public static final String SYSTEM_USER = "system";

    private final String user;

    private final String summary;

    public ChangeSummary( final String user, final String summary )
    {
        this.user = user;
        this.summary = summary;
    }

    public String getUser()
    {
        return user;
    }

    public String getSummary()
    {
        return summary;
    }

}
