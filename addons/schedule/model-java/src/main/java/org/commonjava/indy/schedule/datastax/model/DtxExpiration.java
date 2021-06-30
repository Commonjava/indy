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
package org.commonjava.indy.schedule.datastax.model;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.Date;
import java.util.UUID;

@Table( name = "expiration", readConsistency = "QUORUM", writeConsistency = "QUORUM" )
public class DtxExpiration
{

    @PartitionKey
    private Long expirationPID;

    @ClusteringColumn(0)
    private String storekey;

    @ClusteringColumn(1)
    private String jobName;

    @Column
    private UUID scheduleUID;

    @Column
    private Date expirationTime;

    public DtxExpiration() {}

    public DtxExpiration( Long expirationPID, UUID scheduleUID, Date expirationTime, String storekey, String jobName )
    {
        this.expirationPID = expirationPID;
        this.scheduleUID = scheduleUID;
        this.expirationTime = expirationTime;
        this.storekey = storekey;
        this.jobName = jobName;
    }

    public Long getExpirationPID()
    {
        return expirationPID;
    }

    public void setExpirationPID( Long expirationPID )
    {
        this.expirationPID = expirationPID;
    }

    public String getJobName()
    {
        return jobName;
    }

    public void setJobName( String jobName )
    {
        this.jobName = jobName;
    }

    public UUID getScheduleUID()
    {
        return scheduleUID;
    }

    public void setScheduleUID( UUID scheduleUID )
    {
        this.scheduleUID = scheduleUID;
    }

    public String getStorekey()
    {
        return storekey;
    }

    public void setStorekey( String storekey )
    {
        this.storekey = storekey;
    }

    public Date getExpirationTime()
    {
        return expirationTime;
    }

    public void setExpirationTime( Date expirationTime )
    {
        this.expirationTime = expirationTime;
    }

    @Override
    public String toString()
    {
        return "DtxExpiration{" + "expirationPID=" + expirationPID + ", jobName='" + jobName + '\'' + ", storekey='"
                        + storekey + '\'' + ", scheduleUID=" + scheduleUID + ", expirationTime=" + expirationTime + '}';
    }
}
