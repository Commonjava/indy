package org.commonjava.aprox.tensor.conf;

import java.io.File;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.tensor.config.TensorConfig;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "tensor" )
@Named( "use-factory-instead" )
@Alternative
public class AproxTensorConfig
    implements TensorConfig
{

    private static final int DEFAULT_TENSOR_DISCOVERY_TIMEOUT_MILLIS = 30000;

    private static final String DEFAULT_DB_DIRNAME = "tensor";

    // shipping-oriented builds
    private static final String DEFAULT_DEF_WEBFILTER_PRESET = "sob";

    private Long discoveryTimeoutMillis;

    private String dbDir;

    private File databaseDir;

    private String defaultWebFilterPreset = DEFAULT_DEF_WEBFILTER_PRESET;

    @Override
    public long getDiscoveryTimeoutMillis()
    {
        return discoveryTimeoutMillis == null ? DEFAULT_TENSOR_DISCOVERY_TIMEOUT_MILLIS : discoveryTimeoutMillis;
    }

    @ConfigName( "discoveryTimeoutMillis" )
    public void setDiscoveryTimeoutMillis( final long discoveryTimeoutMillis )
    {
        this.discoveryTimeoutMillis = discoveryTimeoutMillis;
    }

    @Override
    public File getDatabaseDir()
    {
        return databaseDir;
    }

    public AproxTensorConfig setDataBasedir( final File basedir )
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

    @Override
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
