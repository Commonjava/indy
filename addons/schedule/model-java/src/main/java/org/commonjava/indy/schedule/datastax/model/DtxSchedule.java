package org.commonjava.indy.schedule.datastax.model;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.Date;
import java.util.Objects;

@Table( name = "schedule", readConsistency = "QUORUM", writeConsistency = "QUORUM" )
public class DtxSchedule
{

    @PartitionKey
    private String storeKey;

    @ClusteringColumn
    private String jobName;

    @Column
    private String jobType;

    @Column
    private Date scheduleTime;

    @Column
    private Long lifespan;

    @Column
    private Boolean expired;

    @Column
    private Long ttl;

    public DtxSchedule() {}

    public DtxSchedule( String storeKey, String jobType, String jobName, Date scheduleTime,
                        Long lifespan )
    {
        this.jobType = jobType;
        this.jobName = jobName;
        this.storeKey = storeKey;
        this.scheduleTime = scheduleTime;
        this.lifespan = lifespan;
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

    public Long getTtl()
    {
        return ttl;
    }

    public void setTtl( Long ttl )
    {
        this.ttl = ttl;
    }

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
}
