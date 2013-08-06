package org.commonjava.aprox.filer.def;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.filer.KeyBasedPathGenerator;
import org.commonjava.aprox.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.maven.galley.cache.FileCacheProvider;
import org.commonjava.maven.galley.spi.cache.CacheProvider;

public class GalleyStorageProvider
{

    @Inject
    private DefaultStorageProviderConfiguration config;

    private CacheProvider cacheProvider;

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
        this.cacheProvider = new FileCacheProvider( config.getStorageRootDirectory(), new KeyBasedPathGenerator() );
    }

    @Produces
    public CacheProvider getCacheProvider()
    {
        return cacheProvider;
    }
}
