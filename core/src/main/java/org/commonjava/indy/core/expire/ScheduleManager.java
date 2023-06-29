/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.expire;

import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;

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

}
