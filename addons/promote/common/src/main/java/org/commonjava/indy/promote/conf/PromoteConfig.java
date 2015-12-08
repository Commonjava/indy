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
package org.commonjava.indy.promote.conf;

import org.commonjava.indy.conf.AbstractIndyConfigInfo;
import org.commonjava.indy.conf.AbstractIndyFeatureConfig;
import org.commonjava.indy.conf.IndyConfigClassInfo;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.InputStream;

@Alternative
@Named( "promote-config" )
@SectionName( PromoteConfig.SECTION )
public class PromoteConfig
{
    @ApplicationScoped
    public static class FeatureConfig
        extends AbstractIndyFeatureConfig<PromoteConfig, PromoteConfig>
    {
        @Inject
        private ConfigInfo info;

        public FeatureConfig()
        {
            super( PromoteConfig.class );
        }

        @Produces
        @Default
        @ApplicationScoped
        public PromoteConfig getAutoProxConfig()
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

    @ApplicationScoped
    public static class ConfigInfo
        extends AbstractIndyConfigInfo
    {
        public ConfigInfo()
        {
            super( PromoteConfig.class );
        }

        @Override
        public String getDefaultConfigFileName()
        {
            return "conf.d/promote.conf";
        }

        @Override
        public InputStream getDefaultConfig()
        {
            return Thread.currentThread()
                         .getContextClassLoader()
                         .getResourceAsStream( "default-promote.conf" );
        }
    }

    public static final String SECTION = "promote";

    public static final String DEFAULT_DIR = "promote";

    public static final String BASEDIR_PARAM = "basedir";

    public static final String ENABLED_PARAM = "enabled";

    private String basedir;

    private boolean enabled = true;

    public PromoteConfig()
    {
    }

    public PromoteConfig( final String basedir, final boolean enabled )
    {
        this.basedir = basedir;
        this.enabled = enabled;
    }

    @ConfigName(PromoteConfig.BASEDIR_PARAM)
    public void setBasedir( final String basedir )
    {
        this.basedir = basedir;
    }

    public String getBasedir()
    {
        return basedir == null ? DEFAULT_DIR : basedir;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    @ConfigName(PromoteConfig.ENABLED_PARAM)
    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

}
