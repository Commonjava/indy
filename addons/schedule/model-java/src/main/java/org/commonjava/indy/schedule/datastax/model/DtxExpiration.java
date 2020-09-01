package org.commonjava.indy.schedule.datastax.model;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

@Table( name = "expiration", readConsistency = "QUORUM", writeConsistency = "QUORUM" )
public class DtxExpiration
{

    @PartitionKey
    private Long expirationPID;

    @ClusteringColumn
    private UUID scheduleUID;

    @Column
    private String jobName;

    @Column
    private String storekey;

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

    @Override
    public String toString()
    {
        return "DtxExpiration{" + "expirationPID=" + expirationPID + ", scheduleUID=" + scheduleUID + ", jobName='"
                        + jobName + '\'' + ", storekey='" + storekey + '\'' + '}';
    }
}
