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
package org.commonjava.aprox.implrepo.conf;

import java.io.File;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxConfigInfo;
import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
import org.commonjava.aprox.conf.AproxConfigClassInfo;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "implied-repos" )
@Alternative
@Named
public class ImpliedRepoConfig
{

    @javax.enterprise.context.ApplicationScoped
    public static class ConfigInfo
        extends AbstractAproxConfigInfo
    {
        public ConfigInfo()
        {
            super( ImpliedRepoConfig.class );
        }

        @Override
        public String getDefaultConfigFileName()
        {
            return new File( AproxConfigInfo.CONF_INCLUDES_DIR, "implied-repos.conf" ).getPath();
        }

        @Override
        public InputStream getDefaultConfig()
        {
            return Thread.currentThread()
                         .getContextClassLoader()
                         .getResourceAsStream( "default-implied-repos.conf" );
        }
    }

    public static final boolean DEFAULT_ENABLED = false;

    public static final boolean DEFAULT_INCLUDE_SNAPSHOT_REPOS = false;

    private Boolean enabled;

    private Boolean includeSnapshotRepos;

    public ImpliedRepoConfig()
    {
    }

    public boolean isEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    @ConfigName( "enabled" )
    public void setEnabled( final Boolean enabled )
    {
        this.enabled = enabled;
    }

    public boolean isIncludeSnapshotRepos()
    {
        return includeSnapshotRepos == null ? DEFAULT_INCLUDE_SNAPSHOT_REPOS : includeSnapshotRepos;
    }

    @ConfigName( "include.snapshots" )
    public void setIncludeSnapshotRepos( final Boolean includeSnapshotRepos )
    {
        this.includeSnapshotRepos = includeSnapshotRepos;
    }

    @javax.enterprise.context.ApplicationScoped
    public static class FeatureConfig
        extends AbstractAproxFeatureConfig<ImpliedRepoConfig, ImpliedRepoConfig>
    {
        @Inject
        private ConfigInfo info;

        public FeatureConfig()
        {
            super( ImpliedRepoConfig.class );
        }

        @Produces
        @Default
        @ApplicationScoped
        public ImpliedRepoConfig getFlatFileConfig()
            throws ConfigurationException
        {
            return getConfig();
        }

        @Override
        public AproxConfigClassInfo getInfo()
        {
            return info;
        }
    }

}
