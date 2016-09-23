/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.filer.def;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.maven.galley.GalleyInitException;
import org.commonjava.maven.galley.cache.infinispan.FastLocalCacheProviderFactory;
import org.commonjava.maven.galley.cache.partyline.PartyLineCacheProviderFactory;
import org.commonjava.maven.galley.cache.routes.RouteSelector;
import org.commonjava.maven.galley.cache.routes.RoutingCacheProviderFactory;
import org.commonjava.maven.galley.io.ChecksummingTransferDecorator;
import org.commonjava.maven.galley.io.checksum.Md5GeneratorFactory;
import org.commonjava.maven.galley.io.checksum.Sha1GeneratorFactory;
import org.commonjava.maven.galley.io.checksum.Sha256GeneratorFactory;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.FilePatternMatcher;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.io.PathGenerator;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.commonjava.maven.galley.transport.htcli.ContentsFilteringTransferDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DefaultGalleyStorageProvider
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    private DefaultStorageProviderConfiguration config;

    @Inject
    private FileEventManager fileEventManager;

    @Inject
    private PathGenerator pathGenerator;

    @NFSOwnerCache
    @Inject
    private CacheHandle<String,String> nfsOwnerCache;

    @Inject
    private SpecialPathManager specialPathManager;

    @ExecutorConfig( named = "indy-fast-local-executor", threads = 5, priority = 2, daemon = true )
    @WeftManaged
    @Inject
    private ExecutorService fastLocalExecutors;

    private TransferDecorator transferDecorator;

    private CacheProvider partyline;

    private RoutingCacheProviderFactory cacheProviderFactory;

    public DefaultGalleyStorageProvider()
    {
    }

    /**
     * @param storageRoot
     * @deprecated - Use {@link #DefaultGalleyStorageProvider(File, File)} instead
     */
    @Deprecated
    public DefaultGalleyStorageProvider( final File storageRoot )
    {
        this.config = new DefaultStorageProviderConfiguration( storageRoot );
        setup();
    }

    public DefaultGalleyStorageProvider( final File storageRoot, final File nfsStorageRoot )
    {
        this.config = new DefaultStorageProviderConfiguration( storageRoot, nfsStorageRoot );
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

        transferDecorator = new ChecksummingTransferDecorator( Collections.singleton( TransferOperation.GENERATE ),
                                                               specialPathManager, new Md5GeneratorFactory(),
                                                               new Sha1GeneratorFactory(),
                                                               new Sha256GeneratorFactory() );
        transferDecorator = new ContentsFilteringTransferDecorator( transferDecorator );

//        final PartyLineCacheProviderConfig cacheProviderConfig =
//                new PartyLineCacheProviderConfig( config.getStorageRootDirectory() );

        final File storeRoot = config.getStorageRootDirectory();

        final PartyLineCacheProviderFactory partylineFac = new PartyLineCacheProviderFactory( storeRoot );

        try
        {
            this.partyline = partylineFac.create( pathGenerator, transferDecorator, fileEventManager );
        }
        catch ( GalleyInitException e )
        {
            final Logger logger = LoggerFactory.getLogger( this.getClass() );
            logger.error( "[Indy] Can not create CacheProvider for some error.", e );
        }

        final File nfsBasedir = config.getNFSStorageRootDirectory();
        if ( nfsBasedir != null )
        {
            if ( !nfsBasedir.exists() )
            {
                nfsBasedir.mkdirs();
            }
            // nfs root can not be created due to some security reason(like permission), will bypass FastLocal provider and use PartyLine
            if ( nfsBasedir.exists() )
            {
                final FastLocalCacheProviderFactory fastLocalFac =
                        new FastLocalCacheProviderFactory( storeRoot, nfsBasedir, nfsOwnerCache.getCache(), fastLocalExecutors );

                this.cacheProviderFactory = new RoutingCacheProviderFactory( new RouteSelector()
                {
                    final Pattern localPattern = Pattern.compile( "^indy:hosted:.*$" );

                    @Override
                    public boolean isDisposable( ConcreteResource resource )
                    {
                        if ( resource != null )
                        {
                            final Location loc = resource.getLocation();
                            if ( loc != null )
                            {
                                final String uri = loc.getUri();
                                return uri != null && localPattern.matcher( uri ).matches();
                            }
                        }
                        return false;
                    }
                }, fastLocalFac, partylineFac );
            }
            else
            {
                logger.warn(
                        "[Indy] nfs base dir {} can not be created correctly due to some unknown reasons, will use partyline cache provider as default",
                        nfsBasedir );
            }
        }
    }

    @Produces
    @Default
    public TransferDecorator getTransferDecorator()
    {
        return transferDecorator;
    }


    private CacheProvider getPartyLineCacheProvider()
    {
        return partyline;
    }

    @Produces
    @Default
    public CacheProvider getCacheProvider()
    {
        CacheProvider routed;
        if ( cacheProviderFactory != null )
        {
            try
            {
                routed = cacheProviderFactory.create( pathGenerator, transferDecorator, fileEventManager );
                logger.debug( "Using routed cache provider {}", routed );
                return routed;
            }
            catch ( GalleyInitException e )
            {
                logger.error( "[Indy] Can not create CacheProvider for some error.", e );
            }
        }

        routed = getPartyLineCacheProvider();
        return routed;
    }
}
