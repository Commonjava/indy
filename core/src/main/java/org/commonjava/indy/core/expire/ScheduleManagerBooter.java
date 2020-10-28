/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.commonjava.indy.action.BootupAction;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.core.conf.IndyDurableStateConfig;
import org.commonjava.indy.core.conf.IndySchedulerConfig;
import org.commonjava.indy.schedule.conf.ScheduleDBConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@ApplicationScoped
public class ScheduleManagerBooter
        implements BootupAction
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Instance<ScheduleManager> scheduleManagers;

    @Inject
    private IndyDurableStateConfig durableConfig;

    @Override
    public void init()
            throws IndyLifecycleException
    {

        for ( ScheduleManager scheduleManager : scheduleManagers )
        {
            if ( durableConfig.getScheduleStorage().equals( IndyDurableStateConfig.STORAGE_CASSANDRA )
                            && scheduleManager instanceof ScheduleDBManager )
            {
                scheduleManagers.get().init();
            }
            else if ( durableConfig.getScheduleStorage().equals( IndyDurableStateConfig.STORAGE_INFINISPAN )
                            && scheduleManager instanceof DefaultScheduleManager )
            {
                scheduleManagers.get().init();
            }

        }

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
