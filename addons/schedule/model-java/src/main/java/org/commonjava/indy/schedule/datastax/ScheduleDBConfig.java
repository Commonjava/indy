package org.commonjava.indy.schedule.datastax;

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

    public ScheduleDBConfig( String keyspace, Integer replicationFactor )
    {
        this.scheduleKeyspace = keyspace;
        this.replicationFactor = replicationFactor;
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
