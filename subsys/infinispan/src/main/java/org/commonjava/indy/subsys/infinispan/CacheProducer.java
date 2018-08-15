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
package org.commonjava.indy.subsys.infinispan;

import com.codahale.metrics.MetricRegistry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.ShutdownAction;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.metrics.IndyMetricsManager;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.infinispan.Cache;
import org.infinispan.commons.marshall.MarshallableTypeHints;
import org.infinispan.configuration.ConfigurationManager;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.commonjava.indy.metrics.IndyMetricsConstants.METER;
import static org.commonjava.indy.metrics.IndyMetricsConstants.getName;
import static org.commonjava.indy.metrics.IndyMetricsConstants.getSupername;
import static org.commonjava.indy.subsys.infinispan.metrics.IspnCheckRegistrySet.INDY_METRIC_ISPN;

/**
 * Created by jdcasey on 3/8/16.
 */
@ApplicationScoped
public class CacheProducer
        implements ShutdownAction
{
    Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String ISPN_XML = "infinispan.xml";

    private EmbeddedCacheManager cacheManager;

    @Inject
    private IndyConfiguration indyConfiguration;

    @Inject
    private IndyMetricsManager metricsManager;

    @Inject
    private IndyMetricsConfig metricsConfig;

    private Map<String, CacheHandle> caches = new ConcurrentHashMap<>();

    protected CacheProducer()
    {
    }

    public CacheProducer( IndyConfiguration indyConfiguration, EmbeddedCacheManager cacheManager )
    {
        this.indyConfiguration = indyConfiguration;
        this.cacheManager = cacheManager;
    }

    @PostConstruct
    public void start()
    {
        // FIXME This is just here to trigger shutdown hook init for embedded log4j in infinispan-embedded-query.
        // FIXES:
        //
        // Thread-15 ERROR Unable to register shutdown hook because JVM is shutting down.
        // java.lang.IllegalStateException: Cannot add new shutdown hook as this is not started. Current state: STOPPED
        //
        new MarshallableTypeHints().getBufferSizePredictor( CacheHandle.class );

        File confDir = indyConfiguration.getIndyConfDir();
        File ispnConf = new File( confDir, ISPN_XML );

        InputStream resouceStream = Thread.currentThread().getContextClassLoader().getResourceAsStream( ISPN_XML );

        String resourceStr = interpolateStrFromStream( resouceStream, "CLASSPATH:" + ISPN_XML );

        if ( ispnConf.exists() )
        {
            try
            {
                InputStream confStream = FileUtils.openInputStream( ispnConf );
                String confStr = interpolateStrFromStream( confStream, ispnConf.getPath() );
                mergedCachesFromConfig( confStr, "CUSTOMER" );
                mergedCachesFromConfig( resourceStr, "CLASSPATH" );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "Cannot read infinispan configuration from file: " + ispnConf, e );
            }
        }
        else
        {
            try
            {
                logger.info( "Using CLASSPATH resource Infinispan configuration:\n\n{}\n\n", resourceStr );
                cacheManager = new DefaultCacheManager(
                        new ByteArrayInputStream( resourceStr.getBytes( StandardCharsets.UTF_8 ) ) );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "Failed to construct ISPN cacheManger due to CLASSPATH xml stream read error.", e );
            }
        }
    }

    /**
     * Retrieve a cache with a pre-defined configuration (from infinispan.xml) or using the default cache config.
     */
    public synchronized <K,V> CacheHandle<K,V> getCache(String named, Class<K> keyClass, Class<V> valueClass )
    {
        if ( cacheManager == null )
        {
            throw new IllegalStateException( "Cannot access CacheManager. Indy seems to be in a state of shutdown." );
        }

        CacheHandle<K, V> handle = caches.get( named );
        if ( handle == null )
        {
            Cache<K, V> cache = cacheManager.getCache( named );

            final String cacheMetricPrefix = metricsManager == null ?
                    null :
                    getSupername( metricsConfig.getNodePrefix(), INDY_METRIC_ISPN, named );

            handle = new CacheHandle( named, cache, metricsManager, cacheMetricPrefix );
            caches.put( named, handle );
        }
        return handle;
    }

    public synchronized Configuration getCacheConfiguration( String name )
    {
        if ( cacheManager == null )
        {
            throw new IllegalStateException( "Cannot access CacheManager. Indy seems to be in a state of shutdown." );
        }
        return cacheManager.getCacheConfiguration( name );
    }

    public synchronized Configuration getDefaultCacheConfiguration()
    {
        if ( cacheManager == null )
        {
            throw new IllegalStateException( "Cannot access CacheManager. Indy seems to be in a state of shutdown." );
        }

        return cacheManager.getDefaultCacheConfiguration();
    }

    public synchronized Configuration setCacheConfiguration( String name, Configuration config )
    {
        if ( cacheManager == null )
        {
            throw new IllegalStateException( "Cannot access CacheManager. Indy seems to be in a state of shutdown." );
        }
        return cacheManager.defineConfiguration( name, config );
    }

    @Override
    public synchronized void stop()
            throws IndyLifecycleException
    {
        caches.forEach( ( name, cacheHandle ) -> cacheHandle.stop() );

        if ( cacheManager != null )
        {
            logger.info( "Stopping Infinispan caches." );
            cacheManager.stop();
            cacheManager = null;
        }
    }

    @Override
    public int getShutdownPriority()
    {
        return 10;
    }

    @Override
    public String getId()
    {
        return "infinispan-caches";
    }

    private String interpolateStrFromStream( InputStream inputStream, String path )
    {
        String configuration;
        try
        {
            configuration = IOUtils.toString( inputStream );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Cannot read infinispan configuration from : " + path, e );
        }

        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.addValueSource( new PropertiesBasedValueSource( System.getProperties() ) );

        try
        {
            configuration = interpolator.interpolate( configuration );
        }
        catch ( InterpolationException e )
        {
            throw new RuntimeException( "Cannot resolve expressions in infinispan configuration from: " + path, e );
        }
        return configuration;
    }

    /**
     * For the ISPN merging, we should involve at least two different xml config scopes here,
     * one is from indy self default resource xml, another one is from customer's config xml.
     *
     * To prevent the error of EmbeddedCacheManager instances configured with same JMX domain,
     * ISPN should enable 'allowDuplicateDomains' attribute for per GlobalConfigurationBuilder build,
     * that will cost more price there for DefaultCacheManager construct and ConfigurationBuilder build.
     *
     * Since what we need here is simply parsing xml inputStreams to the defined configurations that ISPN
     * could accept, then merging the two stream branches into a entire one.
     * What classes this method uses from ISPN are:
     * {@link ConfigurationBuilderHolder}
     * {@link ParserRegistry}
     * {@link ConfigurationManager}
     *
     * @param config
     * @param path
     */
    private void mergedCachesFromConfig( String config, String path )
    {
        if ( cacheManager == null )
        {
            cacheManager = new DefaultCacheManager();
        }

        ConfigurationBuilderHolder holder = ( new ParserRegistry() ).parse( IOUtils.toInputStream( config ) );
        ConfigurationManager manager = new ConfigurationManager( holder );

        for ( String name : manager.getDefinedCaches() )
        {
            if ( cacheManager.getCacheNames().isEmpty() || !cacheManager.getCacheNames().contains( name ) )
            {
                logger.info( "[ISPN xml merge] Define cache: {} from {} config.", name, path );
                cacheManager.defineConfiguration( name, manager.getConfiguration( name ) );
            }
        }
    }

    public EmbeddedCacheManager getCacheManager()
    {
        return cacheManager;
    }
}
