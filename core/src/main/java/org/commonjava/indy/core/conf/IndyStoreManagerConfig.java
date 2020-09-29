package org.commonjava.indy.core.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;

@SectionName("store-manager")
@ApplicationScoped
public class IndyStoreManagerConfig implements IndyConfigInfo
{

    private String keyspace;

    private int replicationFactor;

    public IndyStoreManagerConfig() {}

    public IndyStoreManagerConfig( String keyspace, int replicationFactor )
    {
        this.keyspace = keyspace;
        this.replicationFactor = replicationFactor;
    }

    public String getKeyspace()
    {
        return keyspace;
    }

    @ConfigName( "store.manager.keyspace" )
    public void setKeyspace( String keyspace )
    {
        this.keyspace = keyspace;
    }

    public int getReplicationFactor()
    {
        return replicationFactor;
    }

    @ConfigName( "store.manager.replica" )
    public void setReplicationFactor( int replicationFactor )
    {
        this.replicationFactor = replicationFactor;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "conf.d/store-manager.conf";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-store-manager.conf" );
    }
}
