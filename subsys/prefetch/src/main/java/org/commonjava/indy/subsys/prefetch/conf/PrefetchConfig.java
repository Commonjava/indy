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
package org.commonjava.indy.subsys.prefetch.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;

@SectionName( "prefetch" )
@ApplicationScoped
public class PrefetchConfig
        implements IndyConfigInfo
{

    private static final String INDY_PREFETCH_BATCH_SIZE = "prefetch.batchsize";

    private static final String INDY_PREFETCH_RESCAN_INTERVAL_SECONDS = "prefetch.rescan.interval.seconds";

    private static final String INDY_PREFETCH_RESCAN_SCHEDULE_SECONDS = "prefetch.rescan.schedule.seconds";

    private static final boolean DEFAULT_ENABLED = false;

    private static final int DEFAULT_BATCH_SIZE = 5;

    private static final int DEFAULT_INTERNAL_SECONDS = 24 * 3600;

    private static final int DEFAULT_SCHEDULE_SECONDS = 1;

    private Boolean enabled;

    private Integer batchSize;

    private Integer rescanIntervalSeconds;

    private Integer rescanScheduleSeconds;

    public PrefetchConfig()
    {
    }

    public boolean isEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    @ConfigName( "enabled" )
    public void setEnabled( Boolean enabled )
    {
        this.enabled = enabled;
    }

    public int getBatchSize()
    {
        return batchSize == null ? DEFAULT_BATCH_SIZE : batchSize;
    }

    @ConfigName( INDY_PREFETCH_BATCH_SIZE )
    public void setBatchSize( Integer batchSize )
    {
        this.batchSize = batchSize;
    }

    public Integer getRescanIntervalSeconds()
    {
        return rescanIntervalSeconds == null || rescanIntervalSeconds <= 0 ?
                DEFAULT_INTERNAL_SECONDS :
                rescanIntervalSeconds;
    }

    @ConfigName( INDY_PREFETCH_RESCAN_INTERVAL_SECONDS )
    public void setRescanIntervalSeconds( Integer rescanIntervalSeconds )
    {
        this.rescanIntervalSeconds = rescanIntervalSeconds;
    }

    public Integer getRescanScheduleSeconds()
    {
        return rescanScheduleSeconds == null || rescanIntervalSeconds < 0 ?
                DEFAULT_SCHEDULE_SECONDS :
                rescanScheduleSeconds;
    }

    @ConfigName( INDY_PREFETCH_RESCAN_SCHEDULE_SECONDS )
    public void setRescanScheduleSeconds( Integer rescanScheduleSeconds )
    {
        this.rescanScheduleSeconds = rescanScheduleSeconds;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "prefetch.conf" ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-prefetch.conf" );
    }
}
