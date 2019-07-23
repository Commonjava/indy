/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
