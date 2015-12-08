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
package org.commonjava.indy.folo.conf;

import java.io.File;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.indy.conf.AbstractIndyConfigInfo;
import org.commonjava.indy.conf.AbstractIndyFeatureConfig;
import org.commonjava.indy.conf.IndyConfigClassInfo;
import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "folo" )
@Alternative
@Named
public class FoloConfig
{

    @javax.enterprise.context.ApplicationScoped
    public static class ConfigInfo
        extends AbstractIndyConfigInfo
    {
        public ConfigInfo()
        {
            super( FoloConfig.class );
        }

        @Override
        public String getDefaultConfigFileName()
        {
            return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "folo.conf" ).getPath();
        }

        @Override
        public InputStream getDefaultConfig()
        {
            return Thread.currentThread()
                         .getContextClassLoader()
                         .getResourceAsStream( "default-folo.conf" );
        }
    }

    public static final int DEFAULT_CACHE_TIMEOUT_SECONDS = 120;

    private Integer cacheTimeoutSeconds;

    public FoloConfig()
    {
    }

    public FoloConfig( final Integer cacheTimeoutSeconds )
    {
        this.cacheTimeoutSeconds = cacheTimeoutSeconds;
    }

    public Integer getCacheTimeoutSeconds()
    {
        return cacheTimeoutSeconds == null ? DEFAULT_CACHE_TIMEOUT_SECONDS : cacheTimeoutSeconds;
    }

    @ConfigName( "cache.timeout.seconds" )
    public void setCacheTimeoutSeconds( final Integer cacheTimeoutSeconds )
    {
        this.cacheTimeoutSeconds = cacheTimeoutSeconds;
    }

    @javax.enterprise.context.ApplicationScoped
    public static class FeatureConfig
        extends AbstractIndyFeatureConfig<FoloConfig, FoloConfig>
    {
        @Inject
        private ConfigInfo info;

        public FeatureConfig()
        {
            super( FoloConfig.class );
        }

        @Produces
        @Default
        @ApplicationScoped
        public FoloConfig getFlatFileConfig()
            throws ConfigurationException
        {
            return getConfig();
        }

        @Override
        public IndyConfigClassInfo getInfo()
        {
            return info;
        }
    }

}
