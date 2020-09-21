package org.commonjava.indy.cassandra.data.config;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;

@SectionName( "artifact_store" )
@ApplicationScoped
public class CassandraStoreConfig implements IndyConfigInfo
{

    private String keyspace;

    private int replicationFactor;

    public CassandraStoreConfig() {}

    public CassandraStoreConfig( String keyspace, int replicationFactor )
    {
        this.keyspace = keyspace;
        this.replicationFactor = replicationFactor;
    }

    public String getKeyspace()
    {
        return keyspace;
    }

    @ConfigName( "store.keyspace" )
    public void setKeyspace( String keyspace )
    {
        this.keyspace = keyspace;
    }

    public int getReplicationFactor()
    {
        return replicationFactor;
    }

    @ConfigName( "store.keyspace.replica" )
    public void setReplicationFactor( int replicationFactor )
    {
        this.replicationFactor = replicationFactor;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return null;
    }
}
