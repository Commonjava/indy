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
package org.commonjava.indy.subsys.infinispan.inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.ShutdownAction;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.inject.Production;
import org.commonjava.indy.subsys.infinispan.conf.InfinispanSubsystemConfig;
import org.commonjava.indy.subsys.infinispan.inject.qualifer.ContentIndexCache;
import org.infinispan.Cache;
import org.infinispan.cdi.ConfigureCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.io.GridFile;
import org.infinispan.io.GridFilesystem;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.commonjava.indy.subsys.infinispan.conf.InfinispanSubsystemConfig.ISPN_XML;

/**
 * Created by jdcasey on 3/8/16.
 */
@ApplicationScoped
public class CacheProducer
        implements ShutdownAction
{
    private EmbeddedCacheManager cacheManager;

    @Inject
    private IndyConfiguration indyConfiguration;

    @Inject
    private InfinispanSubsystemConfig ispnConfig;

    private Map<String, GridFilesystem> filesystems = new HashMap<>();

    private CacheProducer()
    {
    }

    @PostConstruct
    public void start()
    {
        File confDir = indyConfiguration.getIndyConfDir();
        File ispnConf = ispnConfig.getInfinispanXml();
        if ( ispnConf == null )
        {
            ispnConf = new File( confDir, ISPN_XML );
        }

        String configuration;
        if ( ispnConf.exists() )
        {
            try
            {
                configuration = FileUtils.readFileToString( ispnConf );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "Cannot read infinispan configuration from file: " + ispnConf, e );
            }
        }
        else
        {
            try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( ISPN_XML ))
            {
                configuration = IOUtils.toString( stream );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "Cannot read infinispan configuration from classpath: " + ISPN_XML, e );
            }
        }

        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.addValueSource( new PropertiesBasedValueSource( System.getProperties() ) );

        try
        {
            configuration = interpolator.interpolate( configuration );
        }
        catch ( InterpolationException e )
        {
            throw new RuntimeException( "Cannot resolve expressions in infinispan configuration.", e );
        }

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "Using Infinispan configuration:\n\n{}\n\n", configuration );

        try
        {
            cacheManager = new DefaultCacheManager(
                    new ByteArrayInputStream( configuration.getBytes( StandardCharsets.UTF_8 ) ) );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Cannot read infinispan configuration.", e );
        }
    }

    @Produces
    @ApplicationScoped
//    @Production
    @Default
    public EmbeddedCacheManager getCacheManager()
    {
        return cacheManager;
    }

    @ConfigureCache( "content-index" )
    @ContentIndexCache
    @Produces
    @ApplicationScoped
    public Configuration contentIndexCacheCfg()
    {
        return cacheManager.getCacheConfiguration( "content-index" );
    }

    @Produces
    public synchronized GridFilesystem getGridFilesystem( InjectionPoint ip )
    {
        IndyGridFS annotation = ip.getAnnotated().getAnnotation( IndyGridFS.class );
        String basename = annotation.value();

        GridFilesystem gfs = filesystems.get( basename );
        if ( gfs == null )
        {
            Cache<String, byte[]> data = getCacheManager().getCache( "fs-" + basename + "-data" );
            Cache<String, GridFile.Metadata> metadata = getCacheManager().getCache( "fs-" + basename + "-metadata" );

            gfs = new GridFilesystem( data, metadata );
            filesystems.put( basename, gfs );
        }

        return gfs;
    }

    @Override
    public void stop()
            throws IndyLifecycleException
    {
        if ( cacheManager != null )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.info( "Stopping Infinispan caches." );
            cacheManager.stop();
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
}
