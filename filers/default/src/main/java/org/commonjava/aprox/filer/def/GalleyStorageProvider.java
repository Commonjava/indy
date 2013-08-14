package org.commonjava.aprox.filer.def;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.filer.KeyBasedPathGenerator;
import org.commonjava.aprox.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.maven.galley.cache.FileCacheProviderConfig;

public class GalleyStorageProvider
{

    @Inject
    private DefaultStorageProviderConfiguration config;

    private FileCacheProviderConfig cacheProviderConfig;

    public GalleyStorageProvider()
    {
    }

    public GalleyStorageProvider( final File storageRoot )
    {
        this.config = new DefaultStorageProviderConfiguration( storageRoot );
        setup();
    }

    @PostConstruct
    public void setup()
    {
        this.cacheProviderConfig =
            new FileCacheProviderConfig( config.getStorageRootDirectory() ).withPathGenerator( new KeyBasedPathGenerator() );
    }

    @Produces
    public FileCacheProviderConfig getCacheProviderConfig()
    {
        return cacheProviderConfig;
    }
}
