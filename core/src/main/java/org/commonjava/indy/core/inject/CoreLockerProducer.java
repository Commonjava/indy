package org.commonjava.indy.core.inject;

import org.commonjava.cdi.util.weft.Locker;
import org.commonjava.indy.model.core.StoreKey;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

public class CoreLockerProducer
{

    private Locker<StoreKey> groupMembershipLocker;

    private Locker<StoreKey> storeContentLocker;

    @PostConstruct
    public void init()
    {
        groupMembershipLocker = new Locker<>();
        storeContentLocker = new Locker<>();
    }

    @GroupMembershipLocks
    @Produces
    @ApplicationScoped
    public Locker<StoreKey> getGroupMembershipLocker()
    {
        return groupMembershipLocker;
    }

    @StoreContentLocks
    @Produces
    @ApplicationScoped
    public Locker<StoreKey> getStoreContentLocker()
    {
        return storeContentLocker;
    }

}
