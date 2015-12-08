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
package org.commonjava.indy.depgraph.conf;

import java.io.File;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.indy.conf.AbstractIndyConfigInfo;
import org.commonjava.indy.conf.AbstractIndyFeatureConfig;
import org.commonjava.indy.conf.IndyConfigClassInfo;
import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.indy.inject.Production;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.web.config.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IndyDepgraphConfigProvider
    extends AbstractIndyFeatureConfig<IndyDepgraphConfig, IndyDepgraphConfig>
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @ApplicationScoped
    public static class IndyDepgraphConfigInfo
        extends AbstractIndyConfigInfo
    {
        public IndyDepgraphConfigInfo()
        {
            super( IndyDepgraphConfig.class );
        }

        @Override
        public String getDefaultConfigFileName()
        {
            return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "depgraph.conf" ).getPath();
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
    private IndyDepgraphConfigInfo info;

    @Inject
    private DataFileManager ffManager;

    private IndyDepgraphConfig config;

    public IndyDepgraphConfigProvider()
    {
        super( IndyDepgraphConfig.class );
    }

    @Produces
    @Production
    @Default
    public synchronized IndyDepgraphConfig getDepgraphConfig()
        throws ConfigurationException
    {
        logger.debug( "Retrieving Indy depgraph configuration." );
        if ( config == null )
        {
            config = getConfig();
            config.setDirectories( ffManager.getDetachedDataBasedir(), ffManager.getDetachedWorkBasedir() );
        }

        return config;
    }

    @Override
    public IndyConfigClassInfo getInfo()
    {
        return info;
    }
}
