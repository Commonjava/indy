package org.commonjava.indy.core.ctl;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.core.change.StoreEnablementListener;
import org.commonjava.indy.core.expire.Expiration;
import org.commonjava.indy.core.expire.ExpirationSet;
import org.commonjava.indy.core.expire.IndySchedulerException;
import org.commonjava.indy.core.expire.ScheduleManager;
import org.commonjava.indy.core.expire.StoreKeyMatcher;
import org.commonjava.indy.model.core.StoreKey;
import org.quartz.impl.matchers.GroupMatcher;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Date;

@ApplicationScoped
public class SchedulerController
{
    @Inject
    private ScheduleManager scheduleManager;

    protected SchedulerController()
    {
    }

    public SchedulerController( ScheduleManager scheduleManager )
    {
        this.scheduleManager = scheduleManager;
    }

    public Expiration getStoreDisableTimeout( StoreKey storeKey )
            throws IndyWorkflowException
    {
        try
        {
            return scheduleManager.findSingleExpiration( new StoreKeyMatcher( storeKey, StoreEnablementListener.DISABLE_TIMEOUT ) );
        }
        catch ( IndySchedulerException e )
        {
            throw new IndyWorkflowException( "Failed to load disable-timeout schedule for: %s. Reason: %s", e, storeKey, e.getMessage() );
        }
    }

    public ExpirationSet getDisabledStores()
            throws IndyWorkflowException
    {
        try
        {
            return scheduleManager.findMatchingExpirations(
                    GroupMatcher.groupEndsWith( StoreEnablementListener.DISABLE_TIMEOUT ) );
        }
        catch ( IndySchedulerException e )
        {
            throw new IndyWorkflowException( "Failed to load disable-timeout schedules. Reason: %s", e, e.getMessage() );
        }
    }

}
