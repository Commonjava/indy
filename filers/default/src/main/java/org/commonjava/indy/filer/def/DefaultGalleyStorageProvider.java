/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import com.datastax.driver.core.Session;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.NamedThreadFactory;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.conf.InternalFeatureConfig;
import org.commonjava.indy.content.IndyChecksumAdvisor;
import org.commonjava.indy.content.SpecialPathSetProducer;
import org.commonjava.indy.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.maven.galley.cache.pathmapped.PathMappedCacheProviderConfig;
import org.commonjava.o11yphant.metrics.api.Meter;
import org.commonjava.o11yphant.metrics.api.MetricRegistry;
import org.commonjava.o11yphant.metrics.api.Timer;
import org.commonjava.o11yphant.metrics.DefaultMetricsManager;
import org.commonjava.indy.subsys.metrics.conf.IndyMetricsConfig;
import org.commonjava.indy.subsys.cassandra.CassandraClient;
import org.commonjava.indy.subsys.cassandra.config.CassandraConfig;
import org.commonjava.maven.galley.GalleyInitException;
import org.commonjava.maven.galley.cache.CacheProviderFactory;
import org.commonjava.maven.galley.cache.partyline.PartyLineCacheProviderFactory;
import org.commonjava.maven.galley.cache.pathmapped.PathMappedCacheProviderFactory;
import org.commonjava.maven.galley.config.TransportManagerConfig;
import org.commonjava.maven.galley.io.ChecksummingTransferDecorator;
import org.commonjava.maven.galley.io.NoCacheTransferDecorator;
import org.commonjava.maven.galley.io.TransferDecoratorManager;
import org.commonjava.maven.galley.io.checksum.ChecksummingDecoratorAdvisor;
import org.commonjava.maven.galley.io.checksum.Md5GeneratorFactory;
import org.commonjava.maven.galley.io.checksum.Sha1GeneratorFactory;
import org.commonjava.maven.galley.io.checksum.Sha256GeneratorFactory;
import org.commonjava.maven.galley.io.checksum.TransferMetadataConsumer;
import org.commonjava.maven.galley.model.FilePatternMatcher;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.io.PathGenerator;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.commonjava.maven.galley.spi.metrics.TimingProvider;
import org.commonjava.maven.galley.transport.htcli.UploadMetadataGenTransferDecorator;
import org.commonjava.storage.pathmapped.config.DefaultPathMappedStorageConfig;
import org.commonjava.storage.pathmapped.config.PathMappedStorageConfig;
import org.commonjava.storage.pathmapped.pathdb.datastax.CassandraPathDB;
import org.commonjava.storage.pathmapped.metrics.MeasuredPathDB;
import org.commonjava.storage.pathmapped.spi.PathDB;
import org.commonjava.storage.pathmapped.spi.PhysicalStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.commonjava.o11yphant.metrics.util.NameUtils.getSupername;
import static org.commonjava.maven.galley.io.checksum.ChecksummingDecoratorAdvisor.ChecksumAdvice.CALCULATE_AND_WRITE;
import static org.commonjava.maven.galley.io.checksum.ChecksummingDecoratorAdvisor.ChecksumAdvice.NO_DECORATE;
import static org.commonjava.storage.pathmapped.pathdb.datastax.util.CassandraPathDBUtils.*;

@SuppressWarnings( "unused" )
@ApplicationScoped
public class DefaultGalleyStorageProvider
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    private DefaultStorageProviderConfiguration config;

    @Inject
    private IndyConfiguration indyConfiguration;

    @Inject
    private InternalFeatureConfig featureConfig;

    @Inject
    private FileEventManager fileEventManager;

    @Inject
    private PathGenerator pathGenerator;

    @Inject
    private Instance<SpecialPathSetProducer> specialPathSetProducers;

    @Inject
    private SpecialPathManager specialPathManager;

    @ExecutorConfig( named = "galley-delete-executor", threads = 5, priority = 2 )
    @WeftManaged
    @Inject
    private ExecutorService deleteExecutor;

    @Inject
    private TransferMetadataConsumer contentMetadataConsumer;

    @Inject
    private Instance<IndyChecksumAdvisor> checksumAdvisors;

    @Inject
    private Instance<TransferDecorator> transferDecorators;

    @Inject
    private DefaultMetricsManager metricsManager;

    @Inject
    private MetricRegistry metricRegistry;

    @Inject
    private IndyMetricsConfig metricsConfig;

    @Inject
    private CassandraConfig cassandraConfig;

    @Inject
    private CassandraClient cassandraClient;

    private TransportManagerConfig transportManagerConfig;

    private TransferDecoratorManager transferDecorator;

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

        if ( specialPathSetProducers != null )
        {
            specialPathSetProducers.forEach(
                    producer -> {
                        logger.trace( "Adding special paths from: {}", producer.getClass().getName() );
                        specialPathManager.registerSpecialPathSet( producer.getSpecialPathSet() );
                    } );
        }

        setupTransferDecoratorPipeline();
        setupCacheProviderFactory();

        // TODO: Tie this into a config file!
        transportManagerConfig = new TransportManagerConfig();
    }

    private void setupCacheProviderFactory()
    {
        final File storeRoot = config.getStorageRootDirectory();

        if ( indyConfiguration.isStandalone() )
        {
            logger.info( "We're in standalone content-storage mode. Cassandra path-mapping database will NOT be used" );

            // Only work for local debug mode.
            ScheduledExecutorService debugDeleteExecutor = Executors.newScheduledThreadPool( 5, new NamedThreadFactory(
                    "debug-galley-delete-executor", new ThreadGroup( "debug-galley-delete-executor" ), true, 2 ) );
            cacheProviderFactory = new PartyLineCacheProviderFactory( storeRoot, indyConfiguration.isTimeoutProcessing(), debugDeleteExecutor );
            return;
        }

        logger.info( "Initializing Cassandra-based path-mapping database for content storage." );

        PathDB pathDB = null;
        PathMappedStorageConfig pathMappedStorageConfig = getPathMappedStorageConfig();
        if ( cassandraClient != null )
        {
            String keyspace = config.getCassandraKeyspace();
            Session session = cassandraClient.getSession( keyspace );
            if ( session != null )
            {
                logger.info( "Create pathDB, keyspace: {}", keyspace );
                pathDB = new CassandraPathDB( pathMappedStorageConfig, session, keyspace, getReplicationFactor() );
            }
        }

        if ( pathDB != null )
        {
            if ( metricsConfig.isPathDBMetricsEnabled() )
            {
                final String operations = metricsConfig.getPathDBMetricsOperations();
                logger.info( "Create measured PathDB, operations: {}", operations );
                pathDB = new MeasuredPathDB( pathDB, metricsManager, getSupername( "pathDB" ) )
                {
                    @Override
                    protected boolean isMetricEnabled( String metricName )
                    {
                        return isBlank( operations ) || operations.contains( metricName );
                    }
                };
            }
        }

        File legacyBaseDir = config.getLegacyStorageBasedir();
        PhysicalStore physicalStore = new LegacyReadonlyPhysicalStore( storeRoot, legacyBaseDir );

        logger.info( "Create cacheProviderFactory, pathDB: {}, physicalStore: {}", pathDB, physicalStore );
        PathMappedCacheProviderConfig cacheProviderConfig =
                        new PathMappedCacheProviderConfig( storeRoot ).withTimeoutProcessingEnabled(
                                        config.isStorageTimeoutEnabled() );
        cacheProviderFactory =
                        new PathMappedCacheProviderFactory( storeRoot, deleteExecutor, pathMappedStorageConfig, pathDB,
                                                            physicalStore, cacheProviderConfig );
    }

    private PathMappedStorageConfig getPathMappedStorageConfig()
    {
        Map<String, Object> cassandraProps = new HashMap<>();
        cassandraProps.put( PROP_CASSANDRA_HOST, cassandraConfig.getCassandraHost() );
        cassandraProps.put( PROP_CASSANDRA_PORT, cassandraConfig.getCassandraPort() );
        cassandraProps.put( PROP_CASSANDRA_USER, cassandraConfig.getCassandraUser() );
        cassandraProps.put( PROP_CASSANDRA_PASS, cassandraConfig.getCassandraPass() );
        cassandraProps.put( PROP_CASSANDRA_KEYSPACE, config.getCassandraKeyspace() );
        cassandraProps.put( PROP_CASSANDRA_REPLICATION_FACTOR, getReplicationFactor() );

        DefaultPathMappedStorageConfig ret = new DefaultPathMappedStorageConfig( cassandraProps );
        ret.setFileChecksumAlgorithm( config.getFileChecksumAlgorithm() );
        ret.setGcBatchSize( config.getGcBatchSize() );
        ret.setGcGracePeriodInHours( config.getGcGracePeriodInHours() );
        ret.setGcIntervalInMinutes( config.getGcIntervalInMinutes() );
        ret.setDeduplicatePattern( config.getDeduplicatePattern() );
        ret.setPhysicalFileExistenceCheckEnabled( config.isPhysicalFileExistenceCheckEnabled() );

        return ret;
    }

    private int getReplicationFactor()
    {
        Integer replica = config.getCassandraReplicationFactor();
        if ( replica == null )
        {
            replica = indyConfiguration.getKeyspaceReplicas();
        }
        return replica;
    }

    /**
     * The order is important. We put the checksum decorator at the last because some decorators may change the content.
     */
    private void setupTransferDecoratorPipeline()
    {
        List<TransferDecorator> decorators = new ArrayList<>();
        decorators.add( new IOLatencyDecorator( timerProviderFunction(), meterProvider(), cumulativeTimer() ));
        decorators.add( new NoCacheTransferDecorator( specialPathManager ) );
        decorators.add( new UploadMetadataGenTransferDecorator( specialPathManager, timerProviderFunction() ) );
        for ( TransferDecorator decorator : transferDecorators )
        {
            decorators.add( decorator );
        }
        decorators.add( getChecksummingTransferDecorator() );

        transferDecorator = new TransferDecoratorManager( decorators );
    }

    private Function<String, TimingProvider> timerProviderFunction()
    {
        return name-> new IndyTimingProvider( name, metricsManager );
    }

    private BiConsumer<String, Double> cumulativeTimer()
    {
        return (name, elapsed) -> metricsManager.accumulate( name, elapsed );
    }

    private Function<String, Meter> meterProvider()
    {
        return ( name ) -> metricsManager.getMeter( name );
    }

    private Function<String, Timer.Context> timerProvider()
    {
        return ( name ) -> metricsManager.startTimer( name );
    }

    private ChecksummingTransferDecorator getChecksummingTransferDecorator()
    {
        ChecksummingDecoratorAdvisor readAdvisor = ( transfer, op, eventMetadata ) -> {
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

        ChecksummingDecoratorAdvisor writeAdvisor = ( transfer, op, eventMetadata ) -> {
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

        return new ChecksummingTransferDecorator( readAdvisor, writeAdvisor, specialPathManager, timerProviderFunction(),
                                                  contentMetadataConsumer, new Md5GeneratorFactory(),
                                                  new Sha1GeneratorFactory(), new Sha256GeneratorFactory() );
    }

    @Produces
    @Default
    public TransportManagerConfig getTransportManagerConfig()
    {
        return transportManagerConfig;
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
                throw new RuntimeException( "Cannot create CacheProvider", e );
            }
        }

        return cacheProvider;
    }
}
