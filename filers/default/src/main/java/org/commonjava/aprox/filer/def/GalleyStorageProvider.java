/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
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
