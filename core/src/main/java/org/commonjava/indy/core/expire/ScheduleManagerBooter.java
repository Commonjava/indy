package org.commonjava.indy.core.expire;

import org.commonjava.indy.action.BootupAction;
import org.commonjava.indy.action.IndyLifecycleException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@ApplicationScoped
public class ScheduleManagerBooter
        implements BootupAction
{
    @Inject
    private Instance<ScheduleManager> scheduleManager;

    @Override
    public void init()
            throws IndyLifecycleException
    {
        scheduleManager.get().init();
    }

    @Override
    public int getBootPriority()
    {
        return 10;
    }

    @Override
    public String getId()
    {
        return "Schedule Manager";
    }
}
