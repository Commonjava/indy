package org.commonjava.indy.schedule.datastax.model;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Table( name = "schedule", readConsistency = "QUORUM", writeConsistency = "QUORUM" )
public class DtxSchedule
{

    @PartitionKey
    private String storeKey;

    @ClusteringColumn
    private String jobName;

    @Column
    private UUID scheduleUID;

    @Column
    private String jobType;

    @Column
    private Date scheduleTime;

    @Column
    private Long lifespan;

    @Column
    private Boolean expired;

    @Column
    private String payload;

    public DtxSchedule() {}

    public DtxSchedule( String storeKey, String jobType, String jobName, UUID scheduleUID, Date scheduleTime,
                        String payload, Long lifespan )
    {
        this.jobType = jobType;
        this.jobName = jobName;
        this.storeKey = storeKey;
        this.scheduleTime = scheduleTime;
        this.payload = payload;
        this.lifespan = lifespan;
        this.expired = Boolean.FALSE;
        this.scheduleUID = scheduleUID;
    }

    public String getJobType()
    {
        return jobType;
    }

    public void setJobType( String jobType )
    {
        this.jobType = jobType;
    }

    public String getJobName()
    {
        return jobName;
    }

    public void setJobName( String jobName )
    {
        this.jobName = jobName;
    }

    public String getStoreKey()
    {
        return storeKey;
    }

    public void setStoreKey( String storeKey )
    {
        this.storeKey = storeKey;
    }

    public Date getScheduleTime()
    {
        return scheduleTime;
    }

    public void setScheduleTime( Date scheduleTime )
    {
        this.scheduleTime = scheduleTime;
    }

    public Long getLifespan()
    {
        return lifespan;
    }

    public void setLifespan( Long lifespan )
    {
        this.lifespan = lifespan;
    }

    public Boolean getExpired()
    {
        return expired;
    }

    public void setExpired( Boolean expired )
    {
        this.expired = expired;
    }

    public UUID getScheduleUID() { return scheduleUID; }

    public void setScheduleUID( UUID scheduleUID ) { this.scheduleUID = scheduleUID; }

    public String getPayload() { return payload; }

    public void setPayload( String payload ) { this.payload = payload; }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;
        if ( o == null || getClass() != o.getClass() )
            return false;
        DtxSchedule that = (DtxSchedule) o;
        return storeKey.equals( that.storeKey ) && jobName.equals( that.jobName ) && jobType.equals( that.jobType )
                        && scheduleTime.equals( that.scheduleTime ) && lifespan.equals( that.lifespan )
                        && expired.equals( that.expired );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( storeKey, jobName, jobType, scheduleTime, lifespan, expired );
    }

    @Override
    public String toString()
    {
        return "DtxSchedule{" + "storeKey='" + storeKey + '\'' + ", jobName='" + jobName + '\'' + ", scheduleUID="
                        + scheduleUID + ", jobType='" + jobType + '\'' + ", scheduleTime=" + scheduleTime
                        + ", lifespan=" + lifespan + ", expired=" + expired + ", payload='" + payload + '\'' + '}';
    }
}
