package org.commonjava.aprox.tensor.conf;

import java.io.File;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.conf.AbstractAproxMapConfig;
import org.commonjava.aprox.subsys.flatfile.conf.FlatFileConfiguration;
import org.commonjava.tensor.flat.config.FlatFileTensorConfiguration;

@ApplicationScoped
public class AproxTensorCacheConfigProvider
{
    private static final String TENSOR_DATA_BASEDIR = "tensor";

    @ApplicationScoped
    public static class AproxTensorCacheConfigMap
        extends AbstractAproxMapConfig
    {
        public AproxTensorCacheConfigMap()
        {
            super( "tensor-cache" );
        }
    }

    @Inject
    private AproxTensorCacheConfigMap map;

    @Inject
    private FlatFileConfiguration ffConfig;

    private FlatFileTensorConfiguration config;

    @Produces
    @Default
    public synchronized FlatFileTensorConfiguration get()
    {
        if ( config == null )
        {
            final File dir = ffConfig.getStorageDir( TENSOR_DATA_BASEDIR );
            config = new FlatFileTensorConfiguration( dir, map.getConfiguration() );
        }

        return config;
    }
}