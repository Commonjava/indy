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
package org.commonjava.aprox.subsys.infinispan.inject;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.subsys.infinispan.conf.CacheConfiguration;
import org.commonjava.util.logging.Logger;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.Parser;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

@javax.enterprise.context.ApplicationScoped
public class CacheProducer
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CacheConfiguration config;

    @Inject
    private Instance<AproxCacheConfigurator> configurators;

    private EmbeddedCacheManager container;

    @PostConstruct
    public void load()
        throws ProxyDataException
    {
        final File f = new File( config.getPath() );
        if ( !f.exists() )
        {
            throw new ProxyDataException(
                                          "Cannot read infinispan configuration from: {}. Reason: File does not exist.",
                                          config.getPath() );
        }

        logger.info( "\n\n\n\n[APROX-ISPN] Reading Infinispan configuration from: {}", f.getAbsolutePath() );

        FileInputStream fin = null;
        try
        {
            fin = new FileInputStream( f );
            final ConfigurationBuilderHolder cbh = new Parser( Thread.currentThread()
                                                                     .getContextClassLoader() ).parse( fin );

            final GlobalConfigurationBuilder globalConfig = cbh.getGlobalConfigurationBuilder();
            final ConfigurationBuilder defaultConfig = cbh.getDefaultConfigurationBuilder();
            final Map<String, ConfigurationBuilder> namedConfigs = cbh.getNamedConfigurationBuilders();

            if ( configurators != null )
            {
                for ( final AproxCacheConfigurator conf : configurators )
                {
                    logger.info( "Running cache configurator: {}", conf.getClass()
                                                                       .getName() );
                    conf.configure( globalConfig, defaultConfig, namedConfigs );
                }
            }

            container = new DefaultCacheManager( globalConfig.build(), defaultConfig.build() );
            for ( final Map.Entry<String, ConfigurationBuilder> entry : namedConfigs.entrySet() )
            {
                container.defineConfiguration( entry.getKey(), entry.getValue()
                                                                    .build() );
            }

            container.start();
        }
        catch ( final IOException e )
        {
            throw new ProxyDataException( "Cannot read infinispan configuration from: {}. Reason: {}.", e,
                                          config.getPath(), e.getMessage() );
        }
        finally
        {
            closeQuietly( fin );
        }
    }

    @PreDestroy
    public void unload()
    {
        logger.info( "\n\n\n\nSTOPPING INFINISPAN\n\n\n\n\n" );
        if ( container != null )
        {
            container.stop();
        }
    }

    @Produces
    @Default
    @javax.enterprise.context.ApplicationScoped
    public EmbeddedCacheManager getCacheContainer()
    {
        return container;
    }

}
