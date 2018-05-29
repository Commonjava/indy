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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.ShutdownAction;
import org.commonjava.indy.conf.IndyConfiguration;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    private Map<String, CacheHandle> caches = new ConcurrentHashMap<>();

    protected CacheProducer()
    {
    }

    public CacheProducer( IndyConfiguration indyConfiguration )
    {
        this.indyConfiguration = indyConfiguration;
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

        // Load and initiate cacheManager by custom infinispan.xml first
        File ispnConf = new File( indyConfiguration.getIndyConfDir(), ISPN_XML );
        if ( ispnConf.exists() )
        {
            try (InputStream stream = FileUtils.openInputStream( ispnConf ))
            {
                String content = interpolateFromStream( stream );
                defineCaches( content, ispnConf.toString() );
            }
            catch ( Exception e )
            {
                throw new RuntimeException( "Failed load caches from file: " + ispnConf, e );
            }
        }

        // Load and define caches by default infinispan.xml
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( ISPN_XML ))
        {
            String content = interpolateFromStream( stream );
            defineCaches( content, "CLASSPATH:" + ISPN_XML );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Failed to load default infinispan.xml", e );
        }

        if (logger.isDebugEnabled() )
        {
            cacheManager.getCacheNames().forEach( (name) -> {
                logger.debug( "[ISPN] Cache: {}, configuration: {}", name, cacheManager.getCacheConfiguration( name ) );
            } );
        }
    }

    /**
     * Retrieve a cache with a pre-defined configuration (from infinispan.xml) or using the default cache config.
     */
    public synchronized <K,V> CacheHandle<K,V> getCache(String named, Class<K> keyClass, Class<V> valueClass )
    {
        CacheHandle<K, V> handle = caches.get( named );
        if ( handle == null )
        {
            Cache<K, V> cache = cacheManager.getCache( named );
            handle = new CacheHandle( named, cache );
            caches.put( named, handle );
        }
        return handle;
    }

    public synchronized Configuration getCacheConfiguration( String name )
    {
        return cacheManager.getCacheConfiguration( name );
    }

    public synchronized Configuration getDefaultCacheConfiguration()
    {
        return cacheManager.getDefaultCacheConfiguration();
    }

    public synchronized Configuration setCacheConfiguration( String name, Configuration config )
    {
        return cacheManager.defineConfiguration( name, config );
    }

    @Override
    public synchronized void stop()
            throws IndyLifecycleException
    {
        logger.info( "Stop Infinispan caches." );

        caches.forEach( ( name, cacheHandle ) -> cacheHandle.stop() );

        if ( cacheManager != null )
        {
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

    private String interpolateFromStream( InputStream inputStream ) throws IOException, InterpolationException
    {
        String configuration = IOUtils.toString( inputStream );

        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.addValueSource( new PropertiesBasedValueSource( System.getProperties() ) );

        return interpolator.interpolate( configuration );
    }

    private void defineCaches( String content, String path ) throws IOException
    {
        InputStream stream = IOUtils.toInputStream( content );

        if ( cacheManager == null )
        {
            logger.info( "[ISPN] Define caches, from: {}", path );
            cacheManager = new DefaultCacheManager( stream );
            return;
        }

        ConfigurationBuilderHolder holder = ( new ParserRegistry() ).parse( stream );
        ConfigurationManager configurationManager = new ConfigurationManager( holder );

        Set<String> cacheNames = cacheManager.getCacheNames();

        for ( String name : configurationManager.getDefinedCaches() )
        {
            if ( !cacheNames.contains( name ) )
            {
                logger.info( "[ISPN] Define cache: {}, from: {}", name, path );
                cacheManager.defineConfiguration( name, configurationManager.getConfiguration( name ) );
            }
        }
    }

    public EmbeddedCacheManager getCacheManager()
    {
        return cacheManager;
    }

}
