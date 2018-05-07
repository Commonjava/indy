package org.commonjava.indy.subsys.git;

import java.util.Collection;
import java.util.Date;

/**
 * Created by ruhan on 5/7/18.
 */
public class ChangelogEntry
{
    private String username;

    private String message;

    private Collection<String> paths; // file(s) affected

    private Date timestamp;

    public ChangelogEntry( String user, String message, Collection<String> paths )
    {
        this.username = user;
        this.message = message;
        this.paths = paths;
        this.timestamp = new Date();
    }

    public String getUsername()
    {
        return username;
    }

    public String getMessage()
    {
        return message;
    }

    public Collection<String> getPaths()
    {
        return paths;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }
}
