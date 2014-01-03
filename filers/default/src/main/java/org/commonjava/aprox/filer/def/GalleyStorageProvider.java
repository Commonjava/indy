/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.filer.def;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.maven.galley.cache.FileCacheProviderConfig;

public class GalleyStorageProvider
{

    @Inject
    private DefaultStorageProviderConfiguration config;

    private FileCacheProviderConfig cacheProviderConfig;

    //    private KeyBasedPathGenerator pathGen;

    //    private NoOpTransferDecorator transferDecorator;

    //    private AproxFileEventManager fileEvents;

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
        //        this.pathGen = new KeyBasedPathGenerator();
        //        this.transferDecorator = new NoOpTransferDecorator();
        //        this.fileEvents = new AproxFileEventManager();
        this.cacheProviderConfig = new FileCacheProviderConfig( config.getStorageRootDirectory() );
    }

    //    @Produces
    //    public KeyBasedPathGenerator getPathGenerator()
    //    {
    //        return pathGen;
    //    }

    //    @Produces
    //    public TransferDecorator getTransferDecorator()
    //    {
    //        return transferDecorator;
    //    }

    //    @Produces
    //    public FileEventManager getFileEventManager()
    //    {
    //        return fileEvents;
    //    }

    @Produces
    public FileCacheProviderConfig getCacheProviderConfig()
    {
        return cacheProviderConfig;
    }
}
