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
package org.commonjava.indy.schedule.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.indy.conf.SystemPropertyProvider;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.util.Properties;

@SectionName( "scheduledb" )
@ApplicationScoped
public class ScheduleDBConfig implements IndyConfigInfo
{

    private String scheduleKeyspace;

    private int replicationFactor;

    private long partitionKeyRange;

    private long scheduleRatePeriod;

    private int offsetHours;

    private Boolean enabled;

    public ScheduleDBConfig () {}

    public ScheduleDBConfig( String keyspace, int replicationFactor, long partitionKeyRange, long period )
    {
        this.scheduleKeyspace = keyspace;
        this.replicationFactor = replicationFactor;
        this.partitionKeyRange = partitionKeyRange;
        this.scheduleRatePeriod = period;
    }

    public String getScheduleKeyspace()
    {
        return scheduleKeyspace;
    }

    @ConfigName( "schedule.keyspace" )
    public void setScheduleKeyspace( String scheduleKeyspace )
    {
        this.scheduleKeyspace = scheduleKeyspace;
    }

    public long getPartitionKeyRange()
    {
        return partitionKeyRange;
    }

    @ConfigName( "schedule.partition.range" )
    public void setPartitionKeyRange( long partitionKeyRange )
    {
        this.partitionKeyRange = partitionKeyRange;
    }

    public Boolean isEnabled() { return enabled; }

    @ConfigName( "enabled" )
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public long getScheduleRatePeriod() { return scheduleRatePeriod; }

    @ConfigName( "schedule.rate.period" )
    public void setScheduleRatePeriod( long scheduleRatePeriod ) { this.scheduleRatePeriod = scheduleRatePeriod; }

    public int getOffsetHours()
    {
        return offsetHours;
    }

    @ConfigName( "schedule.hours.offset" )
    public void setOffsetHours( int offsetHours )
    {
        this.offsetHours = offsetHours;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "conf.d/schedule.conf";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-scheduledb.conf" );
    }

}
