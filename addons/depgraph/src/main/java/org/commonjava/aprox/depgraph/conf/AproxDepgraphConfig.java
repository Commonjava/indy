package org.commonjava.aprox.depgraph.conf;

import java.io.File;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "depgraph" )
@Named( "use-factory-instead" )
@Alternative
public class AproxDepgraphConfig
{

    private static final int DEFAULT_TENSOR_DISCOVERY_TIMEOUT_MILLIS = 30000;

    private static final String DEFAULT_DB_DIRNAME = "depgraph";

    // shipping-oriented builds
    private static final String DEFAULT_DEF_WEBFILTER_PRESET = "sob";

    private Long discoveryTimeoutMillis;

    private String dbDir;

    private File databaseDir;

    private String defaultWebFilterPreset = DEFAULT_DEF_WEBFILTER_PRESET;

    public long getDiscoveryTimeoutMillis()
    {
        return discoveryTimeoutMillis == null ? DEFAULT_TENSOR_DISCOVERY_TIMEOUT_MILLIS : discoveryTimeoutMillis;
    }

    @ConfigName( "discoveryTimeoutMillis" )
    public void setDiscoveryTimeoutMillis( final long discoveryTimeoutMillis )
    {
        this.discoveryTimeoutMillis = discoveryTimeoutMillis;
    }

    public File getDatabaseDir()
    {
        return databaseDir;
    }

    public AproxDepgraphConfig setDataBasedir( final File basedir )
    {
        this.databaseDir = new File( basedir, getDbDir() );
        return this;
    }

    private String getDbDir()
    {
        return dbDir == null ? DEFAULT_DB_DIRNAME : dbDir;
    }

    @ConfigName( "database.dirName" )
    public void setDbDir( final String dbDir )
    {
        this.dbDir = dbDir;
    }

    public String getDefaultWebFilterPreset()
    {
        return defaultWebFilterPreset;
    }

    @ConfigName( "default.webfilter.preset" )
    public void setDefaultWebFilterPreset( final String preset )
    {
        this.defaultWebFilterPreset = preset;
    }

}
