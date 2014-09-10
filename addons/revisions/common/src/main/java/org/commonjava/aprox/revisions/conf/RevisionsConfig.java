package org.commonjava.aprox.revisions.conf;

import org.commonjava.aprox.subsys.git.ConflictStrategy;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "revisions" )
public class RevisionsConfig
{

    private boolean pushEnabled = false;

    private String dataUpstreamUrl;

    private String conflictAction;

    public boolean isPushEnabled()
    {
        return pushEnabled;
    }

    @ConfigName( "push.enabled" )
    public void setPushEnabled( final boolean pushEnabled )
    {
        this.pushEnabled = pushEnabled;
    }

    public String getDataUpstreamUrl()
    {
        return dataUpstreamUrl;
    }

    @ConfigName( "data.upstream.url" )
    public void setDataUpstreamUrl( final String dataUpstreamUrl )
    {
        this.dataUpstreamUrl = dataUpstreamUrl;
    }

    public String getConflictAction()
    {
        return conflictAction;
    }

    @ConfigName( "conflict.action" )
    public void setConflictAction( final String conflictStrategy )
    {
        this.conflictAction = conflictStrategy;
    }

    public ConflictStrategy getConflictStrategy()
    {
        String action = conflictAction;
        if ( action == null )
        {
            action = ConflictStrategy.merge.name();
        }
        else
        {
            action = action.toLowerCase();
        }

        return ConflictStrategy.valueOf( action );
    }

}
