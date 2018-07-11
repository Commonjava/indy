package org.commonjava.indy.content.index.warmer;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ContentWarmerStartupAction
        implements StartupAction
{
    @Inject
    private ContentIndexWarmer warmer;

    @Override
    public void start()
            throws IndyLifecycleException
    {
        warmer.warmCaches();
    }

    @Override
    public int getStartupPriority()
    {
        return 10;
    }

    @Override
    public String getId()
    {
        return "Content index warmer";
    }
}
