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
package org.commonjava.aprox.depgraph.conf;

import java.io.File;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.conf.AbstractAproxConfigInfo;
import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
import org.commonjava.aprox.conf.AproxConfigClassInfo;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.inject.Production;
import org.commonjava.aprox.subsys.datafile.DataFileManager;
import org.commonjava.web.config.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AproxDepgraphConfigProvider
    extends AbstractAproxFeatureConfig<AproxDepgraphConfig, AproxDepgraphConfig>
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @ApplicationScoped
    public static class AproxDepgraphConfigInfo
        extends AbstractAproxConfigInfo
    {
        public AproxDepgraphConfigInfo()
        {
            super( AproxDepgraphConfig.class );
        }

        @Override
        public String getDefaultConfigFileName()
        {
            return new File( AproxConfigInfo.CONF_INCLUDES_DIR, "depgraph.conf" ).getPath();
        }

        @Override
        public InputStream getDefaultConfig()
        {
            return Thread.currentThread()
                         .getContextClassLoader()
                         .getResourceAsStream( "default-depgraph.conf" );
        }
    }

    @Inject
    private AproxDepgraphConfigInfo info;

    @Inject
    private DataFileManager ffManager;

    private AproxDepgraphConfig config;

    public AproxDepgraphConfigProvider()
    {
        super( AproxDepgraphConfig.class );
    }

    @Produces
    @Production
    @Default
    public synchronized AproxDepgraphConfig getDepgraphConfig()
        throws ConfigurationException
    {
        logger.debug( "Retrieving AProx depgraph configuration." );
        if ( config == null )
        {
            config = getConfig();
            config.setDirectories( ffManager.getDetachedDataBasedir(), ffManager.getDetachedWorkBasedir() );
        }

        return config;
    }

    @Override
    public AproxConfigClassInfo getInfo()
    {
        return info;
    }
}
