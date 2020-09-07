package org.commonjava.indy.schedule.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.indy.conf.SystemPropertyProvider;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.util.Properties;

@SectionName( "schedule" )
@ApplicationScoped
public class ScheduleDBConfig implements IndyConfigInfo, SystemPropertyProvider
{

    private String scheduleKeyspace;

    private Integer replicationFactor;

    private Long partitionKeyRange;

    public ScheduleDBConfig( String keyspace, Integer replicationFactor, Long partitionKeyRange )
    {
        this.scheduleKeyspace = keyspace;
        this.replicationFactor = replicationFactor;
        this.partitionKeyRange = partitionKeyRange;
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

    public Integer getReplicationFactor()
    {
        return replicationFactor;
    }

    @ConfigName( "schedule.keyspace.replica" )
    public void setReplicationFactor( Integer replicationFactor )
    {
        this.replicationFactor = replicationFactor;
    }

    public Long getPartitionKeyRange()
    {
        return partitionKeyRange;
    }

    @ConfigName( "schedule.partition.range" )
    public void setPartitionKeyRange( Long partitionKeyRange )
    {
        this.partitionKeyRange = partitionKeyRange;
    }

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

    @Override
    public Properties getSystemPropertyAdditions()
    {
        return null;
    }
}
