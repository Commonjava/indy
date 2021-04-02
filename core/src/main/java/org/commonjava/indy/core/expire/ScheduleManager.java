package org.commonjava.indy.core.expire;

import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;

import java.util.Set;

public interface ScheduleManager
{

    void init();

    void setProxyTimeouts( final StoreKey key, final String path )
                    throws IndySchedulerException;

    void setSnapshotTimeouts( final StoreKey key, final String path )
                    throws IndySchedulerException;

    void rescheduleSnapshotTimeouts( final HostedRepository deploy )
                    throws IndySchedulerException;

    void rescheduleProxyTimeouts( final RemoteRepository repo )
                    throws IndySchedulerException;

    void rescheduleDisableTimeout( final StoreKey key )
                    throws IndySchedulerException;

    Expiration findSingleExpiration( final StoreKey key, final String jobType );

    ExpirationSet findMatchingExpirations( final String jobType );

    String exportScheduler() throws Exception;

    void importScheduler( Set<ScheduleValue> inputStream ) throws Exception;
}
