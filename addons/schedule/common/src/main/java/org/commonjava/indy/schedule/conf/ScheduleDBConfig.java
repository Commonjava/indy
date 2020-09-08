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

    public int getReplicationFactor()
    {
        return replicationFactor;
    }

    @ConfigName( "schedule.keyspace.replica" )
    public void setReplicationFactor( int replicationFactor )
    {
        this.replicationFactor = replicationFactor;
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

    @Override
    public String getDefaultConfigFileName()
    {
        return null;
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return null;
    }
    
}
