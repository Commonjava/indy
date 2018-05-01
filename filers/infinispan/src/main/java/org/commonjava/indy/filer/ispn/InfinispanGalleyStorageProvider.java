/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.filer.ispn;

import java.io.File;
import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.indy.filer.ispn.conf.InfinispanStorageProviderConfiguration;
import org.commonjava.indy.subsys.infinispan.inject.IndyGridFS;
import org.commonjava.maven.galley.cache.infinispan.GridFileSystemCacheProvider;
import org.commonjava.maven.galley.cache.partyline.PartyLineCacheProvider;
import org.commonjava.maven.galley.io.ChecksummingTransferDecorator;
import org.commonjava.maven.galley.io.checksum.Md5GeneratorFactory;
import org.commonjava.maven.galley.io.checksum.Sha1GeneratorFactory;
import org.commonjava.maven.galley.model.FilePatternMatcher;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.io.PathGenerator;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.infinispan.cdi.ConfigureCache;
import org.infinispan.io.GridFilesystem;

@ApplicationScoped
public class InfinispanGalleyStorageProvider
{

    @Inject
    private InfinispanStorageProviderConfiguration config;

    @Inject
    private FileEventManager fileEventManager;

    @Inject
    private PathGenerator pathGenerator;

    @Inject
    private SpecialPathManager specialPathManager;

    @Inject
    @IndyGridFS( "storage")
    private GridFilesystem cacheFs;

    private TransferDecorator transferDecorator;

    private GridFileSystemCacheProvider cacheProvider;

    public InfinispanGalleyStorageProvider()
    {
    }

    public InfinispanGalleyStorageProvider( final File storageRoot )
    {
        this.config = new InfinispanStorageProviderConfiguration( storageRoot );
        setup();
    }

    @PostConstruct
    public void setup()
    {
        SpecialPathInfo infoSpi = SpecialPathInfo.from( new FilePatternMatcher( ".+\\.info" ) )
                                                 .setDecoratable( false )
                                                 .setDeletable( false )
                                                 .setListable( false )
                                                 .setPublishable( false )
                                                 .setRetrievable( false )
                                                 .setStorable( false )
                                                 .build();

        specialPathManager.registerSpecialPathInfo( infoSpi );

        transferDecorator =
            new ChecksummingTransferDecorator( Collections.singleton( TransferOperation.GENERATE ), specialPathManager,
                                               new Md5GeneratorFactory(),
                                               new Sha1GeneratorFactory() );

        this.cacheProvider =
            new GridFileSystemCacheProvider( pathGenerator, fileEventManager, transferDecorator, cacheFs );
    }

    @Produces
    public TransferDecorator getTransferDecorator()
    {
        return transferDecorator;
    }

    @Produces
    public GridFileSystemCacheProvider getCacheProvider()
    {
        return cacheProvider;
    }
}
