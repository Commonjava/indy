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
package org.commonjava.indy.filer.def;

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.PoolWeftExecutorService;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.cdi.util.weft.WeftScheduledExecutor;
import org.commonjava.indy.content.IndyChecksumAdvisor;
import org.commonjava.indy.content.SpecialPathSetProducer;
import org.commonjava.indy.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.maven.galley.GalleyInitException;
import org.commonjava.maven.galley.cache.CacheProviderFactory;
import org.commonjava.maven.galley.cache.infinispan.FastLocalCacheProviderFactory;
import org.commonjava.maven.galley.cache.partyline.PartyLineCacheProviderFactory;
import org.commonjava.maven.galley.cache.routes.RoutingCacheProviderFactory;
import org.commonjava.maven.galley.config.TransportManagerConfig;
import org.commonjava.maven.galley.io.ChecksummingTransferDecorator;
import org.commonjava.maven.galley.io.NoCacheTransferDecorator;
import org.commonjava.maven.galley.io.TransferDecoratorPipeline;
import org.commonjava.maven.galley.io.checksum.ChecksummingDecoratorAdvisor;
import org.commonjava.maven.galley.io.checksum.Md5GeneratorFactory;
import org.commonjava.maven.galley.io.checksum.Sha1GeneratorFactory;
import org.commonjava.maven.galley.io.checksum.Sha256GeneratorFactory;
import org.commonjava.maven.galley.io.checksum.TransferMetadataConsumer;
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
import org.commonjava.maven.galley.transport.htcli.UploadMetadataGenTransferDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.maven.galley.io.checksum.ChecksummingDecoratorAdvisor.ChecksumAdvice.CALCULATE_AND_WRITE;
import static org.commonjava.maven.galley.io.checksum.ChecksummingDecoratorAdvisor.ChecksumAdvice.NO_DECORATE;

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
    private CacheHandle<String, String> nfsOwnerCache;

    @FastLocalFileRemoveCache
    @Inject
    private CacheHandle<String, ConcreteResource> fastLocalFileRemoveCache;

    @Inject
    private Instance<SpecialPathSetProducer> specialPathSetProducers;

    @Inject
    private SpecialPathManager specialPathManager;

    @ExecutorConfig( named = "galley-delete-executor", threads = 5, priority = 2 )
//    @WeftManaged
    @WeftScheduledExecutor
    @Inject
    private ScheduledExecutorService deleteExecutor;

    @Inject
    private TransferMetadataConsumer contentMetadataConsumer;

    @Inject
    private Instance<IndyChecksumAdvisor> checksumAdvisors;

    private TransportManagerConfig transportManagerConfig;

    private TransferDecorator transferDecorator;

    private CacheProvider cacheProvider;

    private CacheProviderFactory cacheProviderFactory;

    public DefaultGalleyStorageProvider()
    {
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

        ChecksummingDecoratorAdvisor readAdvisor = (transfer, op, eventMetadata)->{
            ChecksummingDecoratorAdvisor.ChecksumAdvice result = NO_DECORATE;
            if ( checksumAdvisors != null )
            {
                for ( IndyChecksumAdvisor advisor : checksumAdvisors )
                {
                    Optional<ChecksummingDecoratorAdvisor.ChecksumAdvice> advice =
                            advisor.getChecksumReadAdvice( transfer, op, eventMetadata );

                    if ( advice.isPresent() )
                    {
                        ChecksummingDecoratorAdvisor.ChecksumAdvice checksumAdvice = advice.get();

                        if ( checksumAdvice.ordinal() > result.ordinal() )
                        {
                            result = checksumAdvice;
                            if ( checksumAdvice == CALCULATE_AND_WRITE )
                            {
                                break;
                            }
                        }
                    }
                }
            }

            logger.debug( "Advising {} for {} of: {}", result, op, transfer );
            return result;
        };

        ChecksummingDecoratorAdvisor writeAdvisor = (transfer, op, eventMetadata)->{
            ChecksummingDecoratorAdvisor.ChecksumAdvice result = NO_DECORATE;
            if ( TransferOperation.GENERATE == op )
            {
                result = CALCULATE_AND_WRITE;
            }
            else if ( checksumAdvisors != null )
            {
                for ( IndyChecksumAdvisor advisor : checksumAdvisors )
                {
                    Optional<ChecksummingDecoratorAdvisor.ChecksumAdvice> advice =
                            advisor.getChecksumWriteAdvice( transfer, op, eventMetadata );

                    if ( advice.isPresent() )
                    {
                        ChecksummingDecoratorAdvisor.ChecksumAdvice checksumAdvice = advice.get();
                        if ( checksumAdvice.ordinal() > result.ordinal() )
                        {
                            result = checksumAdvice;
                            if ( checksumAdvice == CALCULATE_AND_WRITE )
                            {
                                break;
                            }
                        }
                    }
                }
            }

            logger.debug( "Advising {} for {} of: {}", result, op, transfer );
            return result;
        };

        if ( specialPathSetProducers != null )
        {
            specialPathSetProducers.forEach(
                    producer -> {
                        logger.trace( "Adding special paths from: {}", producer.getClass().getName() );
                        specialPathManager.registerSpecialPathSet( producer.getSpecialPathSet() );
                    } );
        }

        transferDecorator = new TransferDecoratorPipeline(
                new ChecksummingTransferDecorator( readAdvisor, writeAdvisor,
                                                   specialPathManager, contentMetadataConsumer,
                                                   new Md5GeneratorFactory(), new Sha1GeneratorFactory(),
                                                   new Sha256GeneratorFactory() ),
                new ContentsFilteringTransferDecorator(),
                new NoCacheTransferDecorator( specialPathManager ),
                new UploadMetadataGenTransferDecorator( specialPathManager ) );

        final File storeRoot = config.getStorageRootDirectory();

        cacheProviderFactory = new PartyLineCacheProviderFactory( storeRoot, deleteExecutor );

        // TODO: Tie this into a config file!
        transportManagerConfig = new TransportManagerConfig();
    }

    @Produces
    @Default
    public TransportManagerConfig getTransportManagerConfig()
    {
        return transportManagerConfig;
    }

    @Produces
    @Default
    public TransferDecorator getTransferDecorator()
    {
        return transferDecorator;
    }

    @Produces
    @Default
    public synchronized CacheProvider getCacheProvider()
    {
        if ( cacheProvider == null )
        {
            try
            {
                cacheProvider = cacheProviderFactory.create( pathGenerator, transferDecorator, fileEventManager );
                logger.debug( "Using cache provider {}", cacheProvider );
                return cacheProvider;
            }
            catch ( GalleyInitException e )
            {
                logger.error( "[Indy] Can not create CacheProvider for some error.", e );
            }
        }

        return cacheProvider;
    }
}
